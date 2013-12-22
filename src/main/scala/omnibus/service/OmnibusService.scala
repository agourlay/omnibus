package omnibus.service

import akka.actor._
import akka.actor.ActorLogging

import omnibus.service.OmnibusServiceProtocol._
import omnibus.repository._

class OmnibusService(topicRepo: ActorRef, subscriberRepo : ActorRef) extends Actor with ActorLogging {
    
  def receive = {
    case CreateTopic(topic)                      => sender ! createTopic(topic)
    case DeleteTopic(topic)                      => sender ! deleteTopic(topic)
    case CheckTopic(topic)                       => sender ! checkTopic(topic)
    case PublishToTopic(topic, message)          => sender ! publishToTopic(topic, message)
    case HttpSubToTopic(topic, responder, mode)  => httpSubscribeToTopic(topic, responder, mode)
  }

  def createTopic(topic : String) : String = {
    topicRepo ! TopicRepositoryProtocol.CreateTopicActor(topic)
    s"Topic $topic created" 
  } 

  def deleteTopic(topic : String) : String = {
    topicRepo ! TopicRepositoryProtocol.DeleteTopicActor(topic)
    s"Topic $topic deleted" 
  } 

  def checkTopic(topic : String) : Boolean = {
    topicRepo ! TopicRepositoryProtocol.CheckTopicActor(topic) 
    true
  } 

  def publishToTopic(topic : String, message : String) : String = {
    topicRepo ! TopicRepositoryProtocol.PublishToTopicActor(topic, message)
    s"Publising message to topic $topic" 
  } 

  def httpSubscribeToTopic(topicName : String, responder : ActorRef, mode : String) {
    subscriberRepo ! SubscriberRepositoryProtocol.CreateHttpSub(splitMultiTopic(topicName), responder)
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