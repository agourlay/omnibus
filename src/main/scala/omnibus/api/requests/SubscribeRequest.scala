package omnibus.api.request

import akka.actor._

import spray.routing._

import omnibus.domain.topic._
import omnibus.domain.subscriber._
import omnibus.repository._

class SubscribeRequest(topicPath: TopicPath, reactiveCmd: ReactiveCmd, ip: String, ctx : RequestContext
                     , subRepo : ActorRef, topicRepo: ActorRef) extends RestRequest(ctx) {
  
  var pending: Set[TopicPath] = Set.empty[TopicPath] 
  var ack: Set[ActorRef] = Set.empty[ActorRef]

  val topics = TopicPath.multi(topicPath.prettyStr)
  topics foreach { topic =>
    pending += topic
    topicRepo ! TopicRepositoryProtocol.LookupTopic(topic)
  }
  
  override def receive = receiveTopicPathRef orElse handleTimeout

  def receiveTopicPathRef : Receive = {
    case TopicPathRef(topicPath, optRef) => handleTopicPathRef(topicPath, optRef)
  }

  def handleTopicPathRef(topicPath: TopicPath, topicRef : Option[ActorRef]) = topicRef match {
    case None      => {
      ctx.complete(new TopicNotFoundException(topicPath.prettyStr))
      self ! PoisonPill
    }  
    case Some(ref) => {
      ack += ref
      if (ack.size == pending.size) {
        subRepo ! SubscriberRepositoryProtocol.CreateSub(ack, ctx.responder, reactiveCmd, ip)
        self ! PoisonPill
      }
    }  
  }
}

object SubscribeRequest {
   def props(topicPath: TopicPath, reactiveCmd: ReactiveCmd, ip: String, ctx : RequestContext, subRepo : ActorRef, topicRepo: ActorRef) 
     = Props(classOf[SubscribeRequest], topicPath, reactiveCmd, ip, ctx, subRepo , topicRepo).withDispatcher("requests-dispatcher")
}