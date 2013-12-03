package omnibus.service

import akka.actor._
import akka.actor.ActorLogging

import omnibus.service.SubscriptionServiceProtocol._
import omnibus.service.TopicServiceProtocol._


class SubscriptionService(topicService: ActorRef) extends Actor with ActorLogging {
    
  def receive = {
     case HttpSubscribeToTopic(topic, responder)  => sender ! "Topic created"
  }

}

object SubscriptionServiceProtocol {
  case class SubscribeToTopic(topicName : String, subscriber : ActorRef)
  case class HttpSubscribeToTopic(topicName : String, responder : ActorRef)
  case class UnsubscribeFromTopic(topicName : String, subscriber : ActorRef)
}