package omnibus.domain.topic

import akka.actor._
import akka.pattern._
import akka.persistence._

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps

import spray.caching.{ LruCache, Cache }

import omnibus.metrics.Instrumented
import omnibus.configuration.Settings
import omnibus.domain.topic._
import omnibus.api.streaming.sse.HttpTopicLeaves
import omnibus.domain.topic.TopicRepositoryProtocol._

class TopicRepository extends PersistentActor with ActorLogging with Instrumented {

  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(context.system).Timeout)

  override def persistenceId = self.path.toStringWithoutAddress

  var rootTopics = Map.empty[String, ActorRef]

  val rootTopicsNumber = metrics.gauge("root-topics")(rootTopics.size)
  val topicsNumber = metrics.counter("topics")

  val openCbMeter = metrics.meter("cb-open")
  val closeCbMeter = metrics.meter("cb-close")
  val halfCbMeter = metrics.meter("cb-half")

  var state = TopicRepoState()
  def updateState(msg: TopicRepoStateValue): Unit = { state = state.update(msg) }

  // cache of most looked up topicRef, conf values to be tuned
  val mostAskedTopic: Cache[ActorRef] = LruCache(maxCapacity = 100, timeToLive = 10 minute)

  val cb = new CircuitBreaker(context.system.scheduler,
    maxFailures = 5,
    callTimeout = timeout.duration,
    resetTimeout = timeout.duration * 10).onOpen(circuitBreakerOpen())
    .onClose(circuitBreakerClose())
    .onHalfOpen(circuitBreakerHalf())

  def circuitBreakerOpen() {
    openCbMeter.mark()
    log.warning("CircuitBreaker is now open")
  }

  def circuitBreakerClose() {
    closeCbMeter.mark()
    log.warning("CircuitBreaker is now closed")
  }

  def circuitBreakerHalf() {
    halfCbMeter.mark()
    log.warning("CircuitBreaker is now half-open")
  }

  val receiveRecover: Receive = {
    case t: TopicRepoStateValue ⇒ {
      createTopic(t.topicPath, self) //ack on self but no used
      updateState(TopicRepoStateValue(t.seqNumber, t.topicPath))
    }
    case SnapshotOffer(_, snapshot: TopicRepoState) ⇒ {
      state = snapshot
      state.events foreach { t ⇒ createTopic(t.topicPath, self) } //ack on self but no used
    }
  }

  val receiveCommand: Receive = {
    case CreateTopic(topic)        ⇒ cb.withSyncCircuitBreaker(persistTopic(topic, sender))
    case DeleteTopic(topic)        ⇒ sender ! cb.withSyncCircuitBreaker(deleteTopic(topic))
    case AllRoots                  ⇒ sender ! cb.withSyncCircuitBreaker(allRoots())
    case AllLeaves(replyTo)        ⇒ allLeaves(replyTo)
    case LookupTopic(topic)        ⇒ lookUpTopic(topic) pipeTo sender()
    case TopicProtocol.Propagation ⇒ log.debug("message propagation reached Repo")
  }

  def persistTopic(topicPath: TopicPath, replyTo: ActorRef) = {
    persistAsync(TopicRepoStateValue(lastSequenceNr + 1, topicPath)) { t ⇒
      createTopic(t.topicPath, replyTo)
      updateState(TopicRepoStateValue(lastSequenceNr, topicPath))
    }
  }

  def createTopic(topicPath: TopicPath, replyTo: ActorRef) = {
    val topicsList = topicPath.segments
    val topicRoot = topicsList.head
    topicsNumber += 1
    if (rootTopics.contains(topicRoot)) {
      log.debug(s"Root topic $topicRoot already exist, forward to sub topic")
      rootTopics(topicRoot) ! TopicProtocol.CreateSubTopic(topicsList.tail, replyTo)
    } else {
      log.debug(s"Creating new root topic $topicRoot")
      val newRootTopic = context.actorOf(Topic.props(topicRoot), topicRoot)
      rootTopics += (topicRoot -> newRootTopic)
      newRootTopic ! TopicProtocol.CreateSubTopic(topicsList.tail, replyTo)
    }
  }

  //FIXME this is somewhat scary...
  def lookUpTopic(topicPath: TopicPath): Future[TopicPathRef] = timing("lookup") {
    val topic = topicPath.prettyStr
    log.debug(s"Lookup in cache for topic $topic")
    val futureOpt: Future[ActorRef] = mostAskedTopic(topicPath) { context.actorSelection(topic).resolveOne }
    futureOpt.map { topicRef ⇒ Some(topicRef) }
      .recover { case e: Exception ⇒ log.debug(s"$e"); mostAskedTopic.remove(topicPath); None }
      .map { optTopicRef ⇒ TopicPathRef(topicPath, optTopicRef) }
  }

  def deleteTopic(topicPath: TopicPath) = {
    val topicName = topicPath.prettyStr
    log.debug(s"trying to delete topic $topicName")
    mostAskedTopic.remove(topicPath)
    if (rootTopics.contains(topicName)) {
      rootTopics -= topicName
    }
    state.events.find(_.topicPath == topicPath) match {
      case None ⇒ log.info(s"Cannot find topic in repo state")
      case Some(topic) ⇒ {
        topicsNumber -= 1
        // FIXME : should not delete message if it is valid
        deleteMessage(topic.seqNumber, true)
        state = TopicRepoState(state.events.filterNot(_.topicPath == topicPath))
      }
    }
    TopicDeletedFromRepo(topicPath)
  }

  def allLeaves(replyTo: ActorRef) {
    context.actorOf(HttpTopicLeaves.props(replyTo, rootTopics.values.toList))
  }

  def allRoots() = {
    val roots = rootTopics.values.toList.map(TopicPathRef(_))
    Roots(roots)
  }
}

object TopicRepositoryProtocol {
  case class CreateTopic(topicName: TopicPath)
  case class DeleteTopic(topicName: TopicPath)
  case class LookupTopic(topicName: TopicPath)
  case class TopicDeletedFromRepo(topicName: TopicPath)
  case class AllLeaves(replyTo: ActorRef)
  case class Roots(refs: List[TopicPathRef])
  case object AllRoots
}

object TopicRepository {
  def props = Props(classOf[TopicRepository]).withDispatcher("topics-dispatcher")
}