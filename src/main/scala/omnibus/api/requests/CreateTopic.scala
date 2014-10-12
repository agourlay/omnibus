package omnibus.api.request

import akka.actor.{ Actor, ActorRef, Props }

import spray.routing._
import spray.json._
import spray.http._
import HttpHeaders._

import DefaultJsonProtocol._

import omnibus.api.endpoint.JsonSupport._
import omnibus.domain.topic._
import omnibus.domain.topic.TopicProtocol._
import omnibus.domain.topic.TopicRepositoryProtocol._

class CreateTopic(topicPath: TopicPath, ctx: RequestContext, topicRepo: ActorRef) extends RestRequest(ctx) {

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = super.receive orElse waitingLookup

  def waitingAck: Receive = {
    case TopicCreated(topicRef) ⇒
      val prettyTopic = topicPath.prettyStr()
      requestOver(StatusCodes.Created, Location(ctx.request.uri) :: Nil, s"Topic $prettyTopic created \n")
  }

  def waitingLookup: Receive = {
    case TopicPathRef(topicPath, topicRef) ⇒ topicRef match {
      case Some(ref) ⇒ requestOver(new TopicAlreadyExistsException(topicPath.prettyStr()))
      case None ⇒
        topicRepo ! TopicRepositoryProtocol.CreateTopic(topicPath)
        context.become(super.receive orElse waitingAck)
    }
  }
}

object CreateTopic {
  def props(topicPath: TopicPath, ctx: RequestContext, topicRepo: ActorRef) = Props(classOf[CreateTopic], topicPath, ctx, topicRepo).withDispatcher("requests-dispatcher")
}