package omnibus.api.request

import akka.actor._

import spray.httpx.SprayJsonSupport._
import spray.routing._
import spray.json._

import DefaultJsonProtocol._

import omnibus.api.endpoint.JsonSupport._
import omnibus.domain.topic._
import omnibus.domain.topic.TopicRepositoryProtocol._

class TopicViewRequest(topicPath: TopicPath, ctx : RequestContext, topicRepo: ActorRef) extends RestRequest(ctx) {

  val prettyTopic = topicPath.prettyStr()

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = waitingLookup orElse handleTimeout

  def waitingTopicView : Receive = {
    case tv : TopicView  => {
      ctx.complete (tv)
      self ! PoisonPill
    }
  }

  def waitingLookup : Receive = {
    case TopicPathRef(topicPath, optRef) => handleTopicPathRef(topicPath, optRef)
  }

  def handleTopicPathRef(topicPath: TopicPath, topicRef : Option[ActorRef]) = topicRef match {
    case Some(ref) => {
      ref ! TopicProtocol.View
      context.become(waitingTopicView orElse handleTimeout)
    }
    case None      => {
      ctx.complete(new TopicNotFoundException(topicPath.prettyStr))
      self ! PoisonPill
    }  
  }
}

object TopicViewRequest {
   def props(topicPath: TopicPath, ctx : RequestContext, topicRepo: ActorRef) 
     = Props(classOf[TopicViewRequest], topicPath, ctx, topicRepo).withDispatcher("requests-dispatcher")
}