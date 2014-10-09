package omnibus.api.streaming.sse

import akka.actor.{ Actor, ActorRef, Props, PoisonPill }

import spray.routing._
import spray.http._

import scala.concurrent.duration._
import scala.language.postfixOps

import omnibus.domain.topic._
import omnibus.domain.topic.TopicRepositoryProtocol._
import omnibus.domain.topic.TopicProtocol._
import omnibus.configuration._

class HttpTopicView(topicPath: TopicPath, ctx: RequestContext, topicRepo: ActorRef) extends ServerSentEventResponse(ctx.responder) {

  implicit def executionContext = context.dispatcher

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = waitingLookup orElse super.receive

  def waitingLookup: Receive = {
    case TopicPathRef(topicPath, optRef) ⇒ handleTopicPathRef(topicPath, optRef)
  }

  def handleTopicPathRef(topicPath: TopicPath, topicRef: Option[ActorRef]) = topicRef match {
    case Some(topicRef) ⇒
      context.system.scheduler.schedule(1.second, 1.second, topicRef, View)
      context.become(handleStream orElse super.receive)
    case None ⇒
      ctx.complete(new TopicNotFoundException(topicPath.prettyStr))
      self ! PoisonPill
  }

  def handleStream: Receive = {
    case topicView: TopicView ⇒ ctx.responder ! toSseChunk(topicView)
  }
}

object HttpTopicStatProtocol {
  object RequestTopicStats
}

object HttpTopicView {
  def props(topicPath: TopicPath, ctx: RequestContext, topicRepo: ActorRef) = Props(classOf[HttpTopicView], topicPath, ctx, topicRepo).withDispatcher("streaming-dispatcher")
}