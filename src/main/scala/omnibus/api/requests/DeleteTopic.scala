package omnibus.api.request

import akka.actor.{ Actor, ActorRef, Props }

import spray.routing._
import spray.http._

import omnibus.domain.topic._
import omnibus.domain.topic.TopicRepositoryProtocol._

class DeleteTopic(topicPath: TopicPath, ctx: RequestContext, topicRepo: ActorRef) extends RestRequest(ctx) {

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = super.receive orElse waitingLookup

  def waitingAck: Receive = {
    case TopicDeletedFromRepo(topicPath) ⇒ {
      val prettyTopic = topicPath.prettyStr()
      requestOver(StatusCodes.Accepted, s"Topic $prettyTopic deleted\n")
    }
  }

  def waitingLookup: Receive = {
    case TopicPathRef(topicPath, topicRef) ⇒
      topicRef match {
        case Some(ref) ⇒
          ref ! TopicProtocol.Delete
          topicRepo ! TopicRepositoryProtocol.DeleteTopic(topicPath)
          context.become(super.receive orElse waitingAck)
        case None ⇒ requestOver(new TopicNotFoundException(topicPath.prettyStr))
      }
  }
}

object DeleteTopic {
  def props(topicPath: TopicPath, ctx: RequestContext, topicRepo: ActorRef) = Props(classOf[DeleteTopic], topicPath, ctx, topicRepo).withDispatcher("requests-dispatcher")
}