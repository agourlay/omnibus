package omnibus.api.request

import akka.actor._

import spray.routing._

import omnibus.domain.topic._
import omnibus.domain.subscriber._
import omnibus.domain.subscriber.SubscriberRepositoryProtocol._
import omnibus.domain.subscriber.SubscriberSupport._

class Subscribe(topicPath: TopicPath, reactiveCmd: ReactiveCmd, ip: String, ctx : RequestContext, subRepo : ActorRef, topicRepo: ActorRef) extends RestRequest(ctx) {
  
  var pending = Set.empty[TopicPath] 
  var ack = Set.empty[ActorRef]

  val topics = TopicPath.multi(topicPath.prettyStr)
  topics foreach { topic =>
    pending += topic
    topicRepo ! TopicRepositoryProtocol.LookupTopic(topic)
  }
  
  override def receive = super.receive orElse receiveTopicPathRef

  def receiveTopicPathRef : Receive = {
    case TopicPathRef(topicPath, optRef) => handleTopicPathRef(topicPath, optRef)
  }

  def handleTopicPathRef(topicPath: TopicPath, topicRef : Option[ActorRef]) = topicRef match {
    case None      => requestOver(new TopicNotFoundException(topicPath.prettyStr))
    case Some(ref) => {
      ack += ref
      if (ack.size == pending.size) {
        subRepo ! SubscriberRepositoryProtocol.CreateSub(ack, ctx.responder, reactiveCmd, ip, SubscriberSupport.SSE)
        closeThings()
      }
    }  
  }
}

object Subscribe {
   def props(topicPath: TopicPath, reactiveCmd: ReactiveCmd, ip: String, ctx : RequestContext, subRepo : ActorRef, topicRepo: ActorRef) 
     = Props(classOf[Subscribe], topicPath, reactiveCmd, ip, ctx, subRepo , topicRepo).withDispatcher("requests-dispatcher")
}