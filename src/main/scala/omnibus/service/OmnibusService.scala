package omnibus.service

import akka.actor._
import akka.actor.ActorLogging
import akka.pattern._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

import omnibus.service.OmnibusServiceProtocol._
import omnibus.repository._
import omnibus.domain._

import reflect.ClassTag

class OmnibusService(topicRepo : ActorRef, subscriberRepo : ActorRef) extends Actor with ActorLogging {
  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(5 seconds) 

  def receive = {
    case CreateTopic(topic, message)                      => sender ! createTopic(topic, message)
    case DeleteTopic(topic)                               => sender ! deleteTopic(topic)
    case CheckTopic(topic)                                => checkTopic(topic, sender)
    case PublishToTopic(topic, message)                   => sender ! publishToTopic(topic, message)
    case SubToTopic(topic, responder, reactiveCmd, http)  => subToTopic(topic, responder, reactiveCmd, http)  pipeTo sender
    case UnsubFromTopic(topic, subscriber)                => sender ! unsubscribeFromTopic(topic, subscriber)
  }

  def createTopic(topic : String, message : String) : String = {
    topicRepo ! TopicRepositoryProtocol.CreateTopicActor(topic)
    if (message.nonEmpty) publishToTopic(topic, message)
    s"Topic $topic created \n" 
  } 

  def deleteTopic(topic : String) : String = {
    topicRepo ! TopicRepositoryProtocol.DeleteTopicActor(topic)
    s"Topic $topic deleted \n" 
  } 

  def checkTopic(topic : String, replyTo : ActorRef) = {
    val bool = (topicRepo ? TopicRepositoryProtocol.CheckTopicActor(topic)).mapTo[Boolean] 
    bool.map( value => if (value) s"Topic $topic exists" else s"Topic $topic does not exist") pipeTo replyTo
  } 

  def publishToTopic(topic : String, message : String) : String = {
    topicRepo ! TopicRepositoryProtocol.PublishToTopicActor(topic, message)
    s"Publishing message to topic $topic \n" 
  } 

  def unsubscribeFromTopic(topic : String, subscriber : ActorRef) {
    // TODO do we need this? 
  }

  def subToTopic(topicName : String, responder : ActorRef, reactiveCmd : ReactiveCmd, httpMode : Boolean) : Future[Boolean] = {
    log.info(s"Request to subscribe to $topicName with reactive cmd $reactiveCmd")
    val p = promise[Boolean]
    val futurResult : Future[Boolean] = p.future

    // Get all future topic ActorRef to subscribe to
    val actorTopics = for (topic <- splitMultiTopic(topicName))
                    yield (topicRepo ? TopicRepositoryProtocol.LookupTopicActor(topic)).mapTo[Option[ActorRef]]
    
    //List[Future[Option]] to Future[List[Option]]                 
    Future.sequence(actorTopics).onComplete {
      case Failure(error)           => {
        log.info("an error occured while subscribing to topic " + error.getMessage())
        promise.failure{new Exception("an error occured while subscribing to topic ")}
      }  
      case Success(optTopicRefList) => {
        val topicRefList : List[ActorRef] = optTopicRefList.filter(_.nonEmpty).map(_.get)
        if (topicRefList.nonEmpty) {
          subscriberRepo ! SubscriberRepositoryProtocol.CreateSub(topicRefList.toSet, responder, reactiveCmd, httpMode) 
          p.success(true)
        } else {
          log.info("Cannot create sub on empty topic list")
          p.failure{new IllegalArgumentException("Cannot create sub on empty topic list")}
        }  
      }  
    }
    futurResult                                               
  }

  def splitMultiTopic(topics : String) : List[String] = topics.split("[/]\\+[/]").toList
}

object OmnibusServiceProtocol {
  case class CreateTopic(topic : String, message : String)
  case class DeleteTopic(topic : String)
  case class CheckTopic(topic : String)
  case class PublishToTopic(topic : String, message : String)
  case class SubToTopic(topic : String, subscriber : ActorRef, reactiveCmd : ReactiveCmd, http : Boolean)
  case class UnsubFromTopic(topic: String, subscriber : ActorRef)
}