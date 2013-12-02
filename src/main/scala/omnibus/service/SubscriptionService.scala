package omnibus.service

import akka.actor._
import akka.actor.ActorLogging

import omnibus.service.SubscriptionServiceProtocol._


class SubscriptionService extends Actor with ActorLogging {
    
  def receive = {
     case SubscribeToTopic(topic, _)  => sender ! "Topic created"
  }

}

object SubscriptionServiceProtocol {
  case class SubscribeToTopic(topicName : String, subscriber : ActorRef)
  case class HttpSubscribeToTopic(topicName : String, subscriber : ActorRef)
  case class UnsubscribeFromTopic(topicName : String, subscriber : ActorRef)
}