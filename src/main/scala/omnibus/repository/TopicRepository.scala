package omnibus.repository

import akka.actor._
import akka.pattern._
import akka.persistence._

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps

import spray.caching.{ LruCache, Cache }

import omnibus.configuration._
import omnibus.domain.topic._
import omnibus.api.streaming.HttpTopicViewStream
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
      createTopic(t.topicPath, self) //ack on self but no used
      updateState(TopicRepoStateValue(t.seqNumber, t.topicPath))
    }  
    case SnapshotOffer(_, snapshot: TopicRepoState) => {
      state = snapshot
      state.events foreach { t => createTopic(t.topicPath, self)} //ack on self but no used
    }  
  }

  val receiveCommand: Receive = {
    case CreateTopic(topic)         => cb.withSyncCircuitBreaker( persistTopic(topic, sender) )
    case DeleteTopic(topic)         => sender ! cb.withSyncCircuitBreaker( deleteTopic(topic) )
    case AllRoots                   => sender ! cb.withSyncCircuitBreaker( allRoots() )
    case AllLeaves(replyTo)         => allLeaves(replyTo) 
    case LookupTopic(topic)         => lookUpTopic(topic) pipeTo sender()
    case TopicProtocol.Propagation  => log.debug("message propagation reached Repo")
  }

  def persistTopic(topicPath: TopicPath, replyTo : ActorRef) = {
    persist(TopicRepoStateValue(lastSequenceNr + 1, topicPath)) { t =>
      createTopic(t.topicPath, replyTo)
      updateState(TopicRepoStateValue(lastSequenceNr, topicPath))
    }
  }  

  def createTopic(topicPath: TopicPath, replyTo : ActorRef) = {
    val topicsList = topicPath.segments
    val topicRoot = topicsList.head
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

  def lookUpTopic(topicPath: TopicPath): Future[TopicPathRef] = {
    val topic = topicPath.prettyStr
    log.debug(s"Lookup in cache for topic $topic")
    val futureOpt: Future[ActorRef] = mostAskedTopic(topicPath) { context.actorSelection(topic).resolveOne }
    futureOpt.map{ topicRef => Some(topicRef) }
             .recover{ case e : Exception => log.debug(s"$e"); mostAskedTopic.remove(topicPath); None }
             .map{ optTopicRef => TopicPathRef(topicPath, optTopicRef) }
  }

  def deleteTopic(topicPath: TopicPath) : TopicDeletedFromRepo = {
    val topicName = topicPath.prettyStr
    log.debug(s"trying to delete topic $topicName")
    mostAskedTopic.remove(topicPath)
    if (rootTopics.contains(topicName)) rootTopics -= topicName
    state.events.find(_.topicPath == topicPath) match {
      case None        => log.info(s"Cannot find topic in repo state")
      case Some(topic) => {
        deleteMessage(topic.seqNumber, true)
        state = TopicRepoState(state.events.filterNot(_.topicPath == topicPath))
      }  
    }   
    TopicDeletedFromRepo(topicPath)
  }

  def allLeaves(replyTo : ActorRef) {
    context.actorOf(HttpTopicViewStream.props(replyTo, rootTopics.values.toList))
  }

  def allRoots() : List[TopicPathRef] = {
    rootTopics.values.toList.map(TopicPathRef(_))
  }
}

object TopicRepositoryProtocol {
  case class CreateTopic(topicName: TopicPath)
  case class DeleteTopic(topicName: TopicPath)
  case class LookupTopic(topicName: TopicPath)
  case class TopicDeletedFromRepo(topicName: TopicPath)
  case class AllLeaves(replyTo : ActorRef)
  case object AllRoots
}

object TopicRepository {
  def props = Props(classOf[TopicRepository]).withDispatcher("topics-dispatcher")
}