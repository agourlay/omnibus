package omnibus.repository

import akka.actor._
import akka.pattern._

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util._

import spray.caching.{ LruCache, Cache }

import omnibus.domain._
import omnibus.configuration._
import omnibus.domain.topic._
import omnibus.repository.TopicRepositoryProtocol._

class TopicRepository extends Actor with ActorLogging {

  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(context.system).Timeout.Ask)

  var rootTopics: Map[String, ActorRef] = Map.empty[String, ActorRef]

  var _seqEventId = 0L
  def nextEventId = {
    val ret = _seqEventId
    _seqEventId += 1
    ret
  }

  // cache of most looked up topicRef, conf values to be tuned
  val mostAskedTopic: Cache[Option[ActorRef]] = LruCache(maxCapacity = 100, timeToLive = 10 minute)

  def receive = {
    case CreateTopicActor(topic)              => createTopic(topic)
    case DeleteTopicActor(topic)              => deleteTopic(topic)
    case CheckTopicActor(topic)               => sender ! checkTopic(topic)
    case LookupTopicActor(topic)              => sender ! lookUpTopicWithCache(topic)
    case PublishToTopicActor(topic, message)  => publishToTopic(topic, message)
    case TopicPastStatActor(topic, replyTo)   => topicPastStat(topic, replyTo)
    case TopicLiveStatActor(topic, replyTo)   => topicLiveStat(topic, replyTo)
    case TopicViewActor(topic, replyTo)       => topicView(topic, replyTo)
    case TopicProtocol.Propagation            => log.debug("message propagation reached TopicRepository")
  }

  def createTopic(topicName: String) = {
    val topicsList = topicName.split('/').toList
    val topicRoot = topicsList.head
    if (rootTopics.contains(topicRoot)) {
      log.debug(s"Root topic $topicRoot already exist, forward to sub topic")
      rootTopics(topicRoot) ! TopicProtocol.CreateSubTopic(topicsList.tail)
    } else {
      log.debug(s"Creating new root topic $topicRoot")
      val newRootTopic = context.actorOf(Topic.props(topicRoot), topicRoot)
      rootTopics += (topicRoot -> newRootTopic)
      newRootTopic ! TopicProtocol.CreateSubTopic(topicsList.tail)
    }
  }

  def publishToTopic(topicName: String, message: String) = {
    lookUpTopicWithCache(topicName) match {
      case Some(topicRef) => topicRef ! TopicProtocol.PublishMessage(Message(nextEventId, topicName, message))
      case None => {
        log.warning(s"trying to push to non existing topic $topicName")
        throw new TopicNotFoundException(topicName)
      }
    }
  }

  def lookUpTopicWithCache(topic: String): Option[ActorRef] = {
    log.debug(s"Lookup in cache for topic $topic")
    val futureOpt: Future[Option[ActorRef]] = mostAskedTopic(topic) { lookUpTopic(topic) }
    // we block here to provide an API based on Option[]
    val optResult : Option[ActorRef] = Await.result(futureOpt, timeout.duration).asInstanceOf[Option[ActorRef]]
    // do not let spray cache insert a none in the cache for the actor
    if (optResult.isEmpty) mostAskedTopic.remove(topic)
    optResult
  }

  def lookUpTopic(topic: String): Future[Option[ActorRef]] = {
    log.debug(s"Lookup for topic $topic")
    val future: Future[ActorRef] = context.actorSelection(topic).resolveOne
    future.map(actor => Some(actor)).recover { case e: ActorNotFound => None }
  }

  def deleteTopic(topicName: String) = {
    log.debug(s"trying to delete topic $topicName")
    lookUpTopicWithCache(topicName) match {
      case Some(topicRef) => topicRef ! TopicProtocol.Delete; mostAskedTopic.remove(topicName)
      case None => log.debug(s"trying to delete a non existing topic $topicName")
    }
  }

  def checkTopic(topicName: String): Boolean = {
    lookUpTopicWithCache(topicName) match {
      case Some(topicRef) => true
      case None => false
    }
  }

  def topicPastStat(topicName: String, replyTo : ActorRef) {
    val p = promise[List[TopicStatisticState]]
    val futurResult= p.future
    lookUpTopicWithCache(topicName) match {
      case None => p.success(List.empty[TopicStatisticState])
      case Some(topicRef) => p.completeWith((topicRef ? TopicStatProtocol.PastStats).mapTo[List[TopicStatisticState]])
    }
    futurResult pipeTo replyTo
  }

  def topicLiveStat(topicName: String, replyTo : ActorRef) {
    val p = promise[TopicStatisticState]
    val futurResult= p.future
    lookUpTopicWithCache(topicName) match {
      case None => p.failure { new TopicNotFoundException(topicName)}
      case Some(topicRef) => p.completeWith((topicRef ? TopicStatProtocol.LiveStats).mapTo[TopicStatisticState])
    }
    futurResult pipeTo replyTo
  }

  def topicView(topicName: String, replyTo : ActorRef) {
    val p = promise[TopicView]
    val futurResult= p.future
    lookUpTopicWithCache(topicName) match {
      case None => p.failure { new TopicNotFoundException(topicName)}
      case Some(topicRef) => p.completeWith((topicRef ? TopicProtocol.View).mapTo[TopicView])
    }
    futurResult pipeTo replyTo
  }  
}

object TopicRepositoryProtocol {
  case class CreateTopicActor(topicName: String)
  case class DeleteTopicActor(topicName: String)
  case class CheckTopicActor(topicName: String)
  case class LookupTopicActor(topicName: String)
  case class PublishToTopicActor(topicName: String, message: String)
  case class TopicPastStatActor(topic: String, replyTo : ActorRef)
  case class TopicLiveStatActor(topic: String, replyTo : ActorRef)
  case class TopicViewActor(topic: String, replyTo : ActorRef)
}


object TopicRepository {
  def props : Props = Props(classOf[TopicRepository])
}