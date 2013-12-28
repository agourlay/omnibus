package omnibus.repository

import akka.actor._

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util._

import spray.caching.{LruCache, Cache}

import omnibus.domain._
import omnibus.repository.TopicRepositoryProtocol._

class TopicRepository extends Actor with ActorLogging {

  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(2 seconds)

  var rootTopics : Map[String, ActorRef] = Map.empty[String, ActorRef]

  var _seqEventId = 0L
  def nextEventId = {
    val ret = _seqEventId
    _seqEventId += 1
    ret
  }

  // cache of most looked up topicRef, conf values to be tuned
  val mostAskedTopic: Cache[Option[ActorRef]] = LruCache(maxCapacity = 100, timeToLive = 1 minute)

  def receive = {
    case CreateTopicActor(topic)             => createTopic(topic)
    case DeleteTopicActor(topic)             => deleteTopic(topic)
    case CheckTopicActor(topic)              => sender ! checkTopic(topic)
    case LookupTopicActor(topic)             => sender ! lookUpTopicWithCache(topic)
    case PublishToTopicActor(topic, message) => publishToTopic(topic, message)
  }

  def createTopic(topicName : String) = {
    val topicsList = topicName.split('/').toList
    val topicRoot = topicsList.head

    if (rootTopics.contains(topicRoot)) {
      log.debug(s"Root topic $topicRoot already exist, forward to sub topic")
      rootTopics(topicRoot) ! TopicProtocol.CreateSubTopic(topicsList.tail)
    } else {
      log.info(s"Creating new root topic $topicRoot")
      val newRootTopic = context.actorOf(Props(classOf[Topic], topicRoot), topicRoot)
      rootTopics += (topicRoot -> newRootTopic)
      newRootTopic ! TopicProtocol.CreateSubTopic(topicsList.tail)
    }
  }

  def publishToTopic(topicName : String, message : String) = {
    lookUpTopicWithCache(topicName) match {
      case Some(topicRef) => topicRef ! TopicProtocol.PublishMessage( Message(nextEventId,topicName, message) )
      case None           => log.debug(s"trying to push to non existing topic $topicName")
    }
  }

  def lookUpTopicWithCache(topic : String) : Option[ActorRef] = {
    log.debug(s"Lookup in cache for topic $topic")
    // TODO can setup None in cache...
    val futureOpt :Future[Option[ActorRef]] = mostAskedTopic(topic){lookUpTopic(topic)}
    // we block here to provide an API based on Option[]
    Await.result(futureOpt, timeout.duration).asInstanceOf[Option[ActorRef]]
  }

  def lookUpTopic(topic : String) : Future[Option[ActorRef]] = {
    log.info(s"Lookup for topic $topic")
    val future : Future[ActorRef] = context.actorSelection(topic).resolveOne
    future.map(actor => Some(actor)).recover{ case e: ActorNotFound => None }
  }

  def deleteTopic(topicName : String) = {
    log.info(s"trying to delete topic $topicName")
    lookUpTopicWithCache(topicName) match {
      case Some(topicRef) => topicRef ! PoisonPill; mostAskedTopic.remove(topicName)
      case None           => log.debug(s"trying to delete a non existing topic $topicName")
    }
  }

  def checkTopic(topicName : String) : Boolean = {
    lookUpTopicWithCache(topicName) match {
      case Some(topicRef) => true
      case None           => false
    }
  }
}

object TopicRepositoryProtocol {
  case class CreateTopicActor(topicName : String)
  case class DeleteTopicActor(topicName : String)
  case class CheckTopicActor(topicName : String)
  case class LookupTopicActor(topicName : String)
  case class PublishToTopicActor(topicName : String, message : String)
}