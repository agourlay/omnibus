package omnibus.service

import akka.actor._
import akka.actor.ActorLogging
import akka.pattern._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

import omnibus.service.OmnibusServiceProtocol._
import omnibus.repository._
import omnibus.configuration._
import omnibus.domain._
import omnibus.domain.topic._
import omnibus.domain.subscriber._

import reflect.ClassTag

class OmnibusService(topicRepo: ActorRef, subscriberRepo: ActorRef) extends Actor with ActorLogging {
  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(context.system).Timeout.Ask)

  def receive = {
    case CreateTopic(topic)                              => sender ! createTopic(topic)
    case DeleteTopic(topic)                              => deleteTopic(topic) pipeTo sender
    case CheckTopic(topic)                               => checkTopic(topic, sender)
    case PublishToTopic(topic, message)                  => publishToTopic(topic, message) pipeTo sender
    case SubToTopic(topic, responder, reactiveCmd, http) => subToTopic(topic, responder, reactiveCmd, http) pipeTo sender
    case UnsubFromTopic(topic, subscriber)               => sender ! unsubscribeFromTopic(topic, subscriber)
    case TopicPastStat(topic)                            => topicRepo ! TopicRepositoryProtocol.TopicPastStatActor(topic, sender)
    case TopicLiveStat(topic)                            => topicRepo ! TopicRepositoryProtocol.TopicLiveStatActor(topic, sender)
    case LookupTopic(topic)                              => lookupTopic(topic) pipeTo sender
    case ViewTopic(topic)                                => topicRepo ! TopicRepositoryProtocol.TopicViewActor(topic, sender)
    case AllLeaves(replyTo)                              => topicRepo ! TopicRepositoryProtocol.AllLeavesActor(replyTo)
    case AllRoots                                        => topicRepo ! TopicRepositoryProtocol.AllRootsActor(sender)
  }

  def createTopic(topic: String) = topicRepo ! TopicRepositoryProtocol.CreateTopicActor(topic)

  def deleteTopic(topic: String): Future[Boolean] = {
    log.debug(s"Deleting topic $topic")
    val p = promise[Boolean]
    val f = p.future
    val exist = (topicRepo ? TopicRepositoryProtocol.CheckTopicActor(topic)).mapTo[Boolean]
    exist.onComplete {
      case Failure(error) => p.failure { new Exception(s"an error occured while deleting $topic \n") }
      case Success(topicExists) => {
        if (topicExists) {
          topicRepo ! TopicRepositoryProtocol.DeleteTopicActor(topic)
          p.success(true)
        } else {
          p.failure { new TopicNotFoundException(topic)}
        }
      }
    }  
    f    
  }

  def checkTopic(topic: String, replyTo: ActorRef) = {
    val bool = (topicRepo ? TopicRepositoryProtocol.CheckTopicActor(topic)).mapTo[Boolean]
    bool pipeTo replyTo
  }

  def checkTopicAsString(topic: String, replyTo: ActorRef) = {
    val bool = (topicRepo ? TopicRepositoryProtocol.CheckTopicActor(topic)).mapTo[Boolean]
    bool.map(value => if (value) s"Topic $topic exists" else s"Topic $topic does not exist") pipeTo replyTo
  }

  def publishToTopic(topic: String, message: String): Future[Boolean] = {
    val p = promise[Boolean]
    val f = p.future
    val exist = (topicRepo ? TopicRepositoryProtocol.CheckTopicActor(topic)).mapTo[Boolean]
    exist.onComplete {
      case Failure(error) => p.failure { new Exception(s"an error occured while pushing to $topic \n") }
      case Success(topicExists) => {
        if (topicExists) {
          topicRepo ! TopicRepositoryProtocol.PublishToTopicActor(topic, message)
          p.success(true)
        } else {
          p.failure { new TopicNotFoundException(topic)}
        }
      }
    }  
    f
  }

  def unsubscribeFromTopic(topic: String, subscriber: ActorRef) {
    // TODO do we need this? 
  }

  def lookupTopic(topic : String) : Future[Option[ActorRef]] = {
    (topicRepo ? TopicRepositoryProtocol.LookupTopicActor(topic)).mapTo[Option[ActorRef]]
  }

  def subToTopic(topicName: String, responder: ActorRef, reactiveCmd: ReactiveCmd, httpMode: Boolean): Future[Boolean] = {
    log.info(s"Request to subscribe to $topicName with reactive cmd $reactiveCmd")
    val p = promise[Boolean]
    val futurResult: Future[Boolean] = p.future

    // Get all future topic ActorRef to subscribe to
    val actorTopics = for (topic <- splitMultiTopic(topicName)) yield lookupTopic(topic)

    //List[Future[Option]] to Future[List[Option]]                 
    Future.sequence(actorTopics).onComplete {
      case Failure(error) => {
        log.warning("an error occured while subscribing to topic " + error.getMessage())
        p.failure { new Exception("an error occured while subscribing to topic ") }
      }
      case Success(optTopicRefList) => {
        // All topics must exist in case of a composed subscriptions
        if (optTopicRefList.forall(_.nonEmpty)) {
          val topicRefList: List[ActorRef] = optTopicRefList.filter(_.nonEmpty).map(_.get)
          subscriberRepo ! SubscriberRepositoryProtocol.CreateSub(topicRefList.toSet, responder, reactiveCmd, httpMode)
          p.success(true)
        } else {
          log.info("None of the topics exist")
          p.failure { new TopicNotFoundException(topicName) }
        }
      }
    }
    futurResult
  }

  def splitMultiTopic(topics: String): List[String] = topics.split("[/]\\+[/]").toList
}

object OmnibusServiceProtocol {
  case class CreateTopic(topic: String)
  case class DeleteTopic(topic: String)
  case class CheckTopic(topic: String)
  case class PublishToTopic(topic: String, message: String)
  case class SubToTopic(topic: String, subscriber: ActorRef, reactiveCmd: ReactiveCmd, http: Boolean)
  case class UnsubFromTopic(topic: String, subscriber: ActorRef)
  case class TopicPastStat(topic: String)
  case class TopicLiveStat(topic: String)
  case class LookupTopic(topic: String)
  case class ViewTopic(topic: String)
  case class AllLeaves(replyTo : ActorRef)
  case object AllRoots
}


object OmnibusService {
  def props(topicRepository:ActorRef, subRepository:ActorRef) : Props = Props(classOf[OmnibusService], topicRepository, subRepository)
}