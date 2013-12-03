package omnibus.service

import akka.actor._
import akka.actor.ActorLogging

import omnibus.service.TopicServiceProtocol._
import omnibus.domain.TopicRepositoryProtocol._


class TopicService(topicRepository: ActorRef) extends Actor with ActorLogging {
    
  def receive = {
    case CreateTopic(topic)              => sender ! createTopic(topic)
    case DeleteTopic(topic)              => sender ! deleteTopic(topic)
    case CheckTopic(topic)               => sender ! checkTopic(topic)
    case PublishToTopic(topic, message)  => sender ! publishToTopic(topic, message)
  }

  def createTopic(topic : String) : String = {
    s"Topic $topic created" 
  } 

  def deleteTopic(topic : String) : String = {
    s"Topic $topic deleted" 
  } 

  def checkTopic(topic : String) : String = {
    s"Checking topic $topic" 
  } 

  def publishToTopic(topic : String, message : String) : String = {
    s"Publising message to topic $topic" 
  } 

}

object TopicServiceProtocol {
  case class CreateTopic(topicName : String)
  case class DeleteTopic(topicName : String)
  case class CheckTopic(topicName : String)
  case class PublishToTopic(topicName : String, message : String)
}