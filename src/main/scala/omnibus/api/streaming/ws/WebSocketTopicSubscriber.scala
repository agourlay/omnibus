package omnibus.api.streaming.ws

import akka.actor.{ Actor, ActorRef, Props }

import spray.routing._

import omnibus.domain.topic._
import omnibus.domain.subscriber._
import omnibus.domain.subscriber.SubscriberRepositoryProtocol._
import omnibus.domain.subscriber.SubscriberSupport._

class WebSocketTopicSubscriber(topicPath: TopicPath, reactiveCmd: ReactiveCmd, ip: String, subRepo: ActorRef, topicRepo: ActorRef) extends Actor {

  var pending = Set.empty[TopicPath]
  var ack = Set.empty[ActorRef]

  val topics = TopicPath.multi(topicPath.prettyStr)
  topics foreach { topic ⇒
    pending += topic
    topicRepo ! TopicRepositoryProtocol.LookupTopic(topic)
  }

  override def receive: Receive = {
    case TopicPathRef(topicPath, optRef) ⇒ handleTopicPathRef(topicPath, optRef)
  }

  def handleTopicPathRef(topicPath: TopicPath, topicRef: Option[ActorRef]) = topicRef match {
    case None ⇒ context.parent ! new TopicNotFoundException(topicPath.prettyStr)
    case Some(ref) ⇒
      ack += ref
      if (ack.size == pending.size) {
        subRepo ! SubscriberRepositoryProtocol.CreateSub(ack, context.parent, reactiveCmd, ip, SubscriberSupport.WS)
      }
  }
}

object WebSocketTopicSubscriber {
  def props(topicPath: TopicPath, reactiveCmd: ReactiveCmd, ip: String, subRepo: ActorRef, topicRepo: ActorRef) = Props(classOf[WebSocketTopicSubscriber], topicPath, reactiveCmd, ip, subRepo, topicRepo).withDispatcher("streaming-dispatcher")
}