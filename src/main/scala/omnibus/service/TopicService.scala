package omnibus.service

import akka.actor.Actor
import akka.actor.ActorLogging

import omnibus.service.TopicServiceProtocol._


class TopicService extends Actor with ActorLogging {
    
  def receive = {
    case CreateTopic(topic)              => sender ! "Create topic " + topic + "\n\n"
    case DeleteTopic(topic)              => sender ! "Delete topic " + topic + "\n\n"
    case CheckTopic(topic)               => sender ! "Check topic " + topic + "\n\n"
    case PublishToTopic(topic, message)  => sender ! "Message " + message + " pushed to" +" topic" + "\n\n"
  }

}

object TopicServiceProtocol {
  case class CreateTopic(topicName : String)
  case class DeleteTopic(topicName : String)
  case class CheckTopic(topicName : String)
  case class PublishToTopic(topicName : String, message : String)
}