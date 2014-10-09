package omnibus.api.request

import akka.actor.{ Actor, ActorRef, Props }

import spray.routing._

import omnibus.domain.topic._
import omnibus.domain.subscriber._
import omnibus.domain.subscriber.SubscriberRepositoryProtocol._
import omnibus.domain.subscriber.SubscriberSupport._
import omnibus.api.streaming.sse.HttpTopicSubscriber

class Subscribe(topicPath: TopicPath, reactiveCmd: ReactiveCmd, ip: String, ctx: RequestContext, subRepo: ActorRef, topicRepo: ActorRef) extends Actor {

  var pending = Set.empty[TopicPath]
  var ack = Set.empty[ActorRef]

  val topics = TopicPath.multi(topicPath.prettyStr)
  topics foreach { topic ⇒
    pending += topic
    topicRepo ! TopicRepositoryProtocol.LookupTopic(topic)
  }

  override def receive = {
    case TopicPathRef(topicPath, optRef) ⇒ handleTopicPathRef(topicPath, optRef)
  }

  def handleTopicPathRef(topicPath: TopicPath, topicRef: Option[ActorRef]) = topicRef match {
    case None ⇒ ctx.complete(new TopicNotFoundException(topicPath.prettyStr))
    case Some(ref) ⇒ {
      ack += ref
      if (ack.size == pending.size) {
        val httpSub = context.actorOf(HttpTopicSubscriber.props(ctx.responder, reactiveCmd))
        subRepo ! SubscriberRepositoryProtocol.CreateSub(ack, httpSub, reactiveCmd, ip, SubscriberSupport.SSE)
      }
    }
  }
}

object Subscribe {
  def props(topicPath: TopicPath, reactiveCmd: ReactiveCmd, ip: String, ctx: RequestContext, subRepo: ActorRef, topicRepo: ActorRef) = Props(classOf[Subscribe], topicPath, reactiveCmd, ip, ctx, subRepo, topicRepo).withDispatcher("requests-dispatcher")
}