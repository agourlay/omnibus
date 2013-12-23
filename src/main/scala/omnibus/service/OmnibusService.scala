package omnibus.service

import akka.actor._
import akka.actor.ActorLogging
import akka.pattern._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

import omnibus.service.OmnibusServiceProtocol._
import omnibus.repository._

import reflect.ClassTag

class OmnibusService(topicRepo: ActorRef, subscriberRepo : ActorRef) extends Actor with ActorLogging {
  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(5 seconds) 

  def receive = {
    case CreateTopic(topic)                      => sender ! createTopic(topic)
    case DeleteTopic(topic)                      => sender ! deleteTopic(topic)
    case CheckTopic(topic)                       => checkTopic(topic, sender)
    case PublishToTopic(topic, message)          => sender ! publishToTopic(topic, message)
    case HttpSubToTopic(topic, responder, mode)  => httpSubscribeToTopic(topic, responder, mode)
  }

  def createTopic(topic : String) : String = {
    topicRepo ! TopicRepositoryProtocol.CreateTopicActor(topic)
    s"Topic $topic created \n" 
  } 

  def deleteTopic(topic : String) : String = {
    topicRepo ! TopicRepositoryProtocol.DeleteTopicActor(topic)
    s"Topic $topic deleted \n" 
  } 

  def checkTopic(topic : String, replyTo : ActorRef) = {
    val bool : Future[Boolean] = (topicRepo ? TopicRepositoryProtocol.CheckTopicActor(topic)).mapTo[Boolean] 
    bool pipeTo replyTo
  } 

  def publishToTopic(topic : String, message : String) : String = {
    topicRepo ! TopicRepositoryProtocol.PublishToTopicActor(topic, message)
    s"Publising message to topic $topic \n" 
  } 

  def httpSubscribeToTopic(topicName : String, responder : ActorRef, mode : String) {
    val actorTopics : List[Future[Option[ActorRef]]] = for (topic <- splitMultiTopic(topicName))
                                               yield (topicRepo ? TopicRepositoryProtocol.LookupTopicActor(topic)).mapTo[Option[ActorRef]]
    Future.sequence(actorTopics).onComplete {
      case Failure(error)        => log.info(error.getMessage())
      case Success(optTopicRefList) => {
        val topicRefList : List[ActorRef] = optTopicRefList.filter(_ != None).map(_.get)
        if (topicRefList.nonEmpty) {
          subscriberRepo ! SubscriberRepositoryProtocol.CreateHttpSub(topicRefList.toSet, responder)
        } else {
          log.info("Cannot create sub on empty topic list")
        }  
      }  
    }                                               
  }

  def splitMultiTopic(topics : String) : List[String] = topics.split("/+").toList

}

object OmnibusServiceProtocol {
  case class CreateTopic(topicName : String)
  case class DeleteTopic(topicName : String)
  case class CheckTopic(topicName : String)
  case class PublishToTopic(topicName : String, message : String)
  case class SubToTopic(topicName : String, subscriber : ActorRef, mode : String)
  case class HttpSubToTopic(topicName : String, responder : ActorRef, mode : String)
  case class UnsubscribeFromTopic(topicName : String, subscriber : ActorRef)
}