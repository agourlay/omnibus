package omnibus.repository

import akka.actor._
import akka.pattern._
import akka.persistence._

import scala.concurrent._
import scala.concurrent.Promise._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util._
import scala.util.control.NoStackTrace

import spray.caching.{ LruCache, Cache }

import omnibus.domain._
import omnibus.configuration._
import omnibus.domain.topic._
import omnibus.http.streaming.HttpTopicViewStream
import omnibus.repository.TopicRepositoryProtocol._

class TopicRepository extends EventsourcedProcessor with ActorLogging {

  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(context.system).Timeout.Ask)

  var rootTopics: Map[String, ActorRef] = Map.empty[String, ActorRef]

  var state = TopicRepoState()
  def updateState(msg: TopicRepoStateValue): Unit = {state = state.update(msg)} 

  // cache of most looked up topicRef, conf values to be tuned
  val mostAskedTopic: Cache[ActorRef] = LruCache(maxCapacity = 100, timeToLive = 10 minute)

  val cb = new CircuitBreaker(context.system.scheduler,
      maxFailures = 5,
      callTimeout = timeout.duration,
      resetTimeout = timeout.duration * 10).onOpen(log.warning("CircuitBreaker is now open"))
                                           .onClose(log.warning("CircuitBreaker is now closed"))
                                           .onHalfOpen(log.warning("CircuitBreaker is now half-open"))
  val receiveRecover: Receive = {
    case t: TopicRepoStateValue                     => {
      createTopic(t.topicPath, promise[Boolean])
      updateState(TopicRepoStateValue(t.seqNumber, t.topicPath))
    }  
    case SnapshotOffer(_, snapshot: TopicRepoState) => {
      state = snapshot
      state.events foreach { t => createTopic(t.topicPath, promise[Boolean])}
    }  
  }

  val receiveCommand: Receive = {
    case CreateTopic(topic)         => cb.withCircuitBreaker( persistTopic(topic) ) pipeTo sender
    case DeleteTopic(topic)         => cb.withCircuitBreaker( deleteTopic(topic) ) pipeTo sender

    case AllRoots                   => sender ! allRoots()
    case AllLeaves(replyTo)         => allLeaves(replyTo) 

    case LookupTopic(topic)         => lookUpTopic(topic) pipeTo sender()
    case TopicProtocol.Propagation  => log.debug("message propagation reached Repo")
  }

  def persistTopic(topicPath: TopicPath) : Future[Boolean] = {
    val p = promise[Boolean]
    persist(TopicRepoStateValue(lastSequenceNr + 1, topicPath)) { t =>
      createTopic(t.topicPath, p)
      updateState(TopicRepoStateValue(lastSequenceNr, topicPath))
    }
    p.future
  }  

  def createTopic(topicPath: TopicPath, promise : Promise[Boolean]) = {
    val topicsList = topicPath.segments
    val topicRoot = topicsList.head
    if (rootTopics.contains(topicRoot)) {
      log.debug(s"Root topic $topicRoot already exist, forward to sub topic")
      rootTopics(topicRoot) ! TopicProtocol.CreateSubTopic(topicsList.tail, promise)
    } else {
      log.debug(s"Creating new root topic $topicRoot")
      val newRootTopic = context.actorOf(Topic.props(topicRoot), topicRoot)
      rootTopics += (topicRoot -> newRootTopic)
      newRootTopic ! TopicProtocol.CreateSubTopic(topicsList.tail, promise)
    }
  }

  def lookUpTopic(topicPath: TopicPath): Future[TopicPathRef] = {
    val topic = topicPath.prettyStr
    log.debug(s"Lookup in cache for topic $topic")
    val futureOpt: Future[ActorRef] = mostAskedTopic(topicPath) { context.actorSelection(topic).resolveOne }
    futureOpt.map{ topicRef => Some(topicRef) }
             .recover{ case e : Exception => log.debug(s"$e"); mostAskedTopic.remove(topicPath); None }
             .map{ optTopicRef => TopicPathRef(topicPath, optTopicRef) }
  }

  def deleteTopic(topicPath: TopicPath) : Future[Boolean] = {
    val topicName = topicPath.prettyStr
    val p = promise[Boolean]
    log.debug(s"trying to delete topic $topicName")
    mostAskedTopic.remove(topicPath)
    if (rootTopics.contains(topicName)) rootTopics -= topicName
    val delete = state.events.find(_.topicPath == topicPath)
    delete match {
      case None        => log.info(s"Cannot find topic in repo state")
      case Some(topic) => {
        deleteMessage(topic.seqNumber, true)
        state = TopicRepoState(state.events.filterNot(_.topicPath == topicPath))
      }  
    }   
    p.success(true)
    p.future
  }

  def allLeaves(replyTo : ActorRef) {
    context.actorOf(HttpTopicViewStream.props(replyTo, rootTopics.values.toList))
  }

  def allRoots() : List[TopicPathRef] = {
    rootTopics.values.toList.map( ref => TopicPathRef(TopicPath(ref), Some(ref)))
  }
}

object TopicRepositoryProtocol {
  case class CreateTopic(topicName: TopicPath)
  case class DeleteTopic(topicName: TopicPath)
  case class LookupTopic(topicName: TopicPath)
  case class AllLeaves(replyTo : ActorRef)
  case object AllRoots
}

object TopicRepository {
  def props = Props(classOf[TopicRepository]).withDispatcher("topics-dispatcher")
}