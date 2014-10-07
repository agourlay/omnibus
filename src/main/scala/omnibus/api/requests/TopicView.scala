package omnibus.api.request

import akka.actor.{ Actor, ActorRef, Props }

import spray.httpx.SprayJsonSupport._
import spray.routing._
import spray.json._

import DefaultJsonProtocol._

import omnibus.api.endpoint.JsonSupport._
import omnibus.domain.topic._
import omnibus.domain.topic.TopicRepositoryProtocol._

class ViewTopic(topicPath: TopicPath, ctx: RequestContext, topicRepo: ActorRef) extends RestRequest(ctx) {

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = super.receive orElse waitingLookup

  def waitingTopicView: Receive = {
    case tv: TopicView ⇒ requestOver(tv)
  }

  def waitingLookup: Receive = {
    case TopicPathRef(topicPath, optRef) ⇒ handleTopicPathRef(topicPath, optRef)
  }

  def handleTopicPathRef(topicPath: TopicPath, topicRef: Option[ActorRef]) = topicRef match {
    case Some(ref) ⇒ {
      ref ! TopicProtocol.View
      context.become(super.receive orElse waitingTopicView)
    }
    case None ⇒ requestOver(new TopicNotFoundException(topicPath.prettyStr))
  }
}

object ViewTopic {
  def props(topicPath: TopicPath, ctx: RequestContext, topicRepo: ActorRef) = Props(classOf[ViewTopic], topicPath, ctx, topicRepo).withDispatcher("requests-dispatcher")
}