package omnibus.api.request

import akka.actor._

import spray.routing._
import spray.http._

import omnibus.domain.topic._
import omnibus.domain.topic.TopicRepositoryProtocol._

class Publish(topicPath: TopicPath, message: String, ctx: RequestContext, topicRepo: ActorRef) extends RestRequest(ctx) {

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = super.receive orElse waitingLookup

  def waitingAck: Receive = {
    case TopicProtocol.MessagePublished => {
      val prettyTopic = topicPath.prettyStr()
      requestOver(StatusCodes.Accepted, s"Message published to topic $prettyTopic\n")
    }
  }

  def waitingLookup: Receive = {
    case TopicPathRef(topicPath, optRef) => handleTopicPathRef(topicPath, optRef)
  }

  def handleTopicPathRef(topicPath: TopicPath, topicRef: Option[ActorRef]) = topicRef match {
    case Some(ref) => {
      ref ! TopicProtocol.PublishMessage(message)
      context.become(super.receive orElse waitingAck)
    }
    case None => requestOver(new TopicNotFoundException(topicPath.prettyStr))
  }
}

object Publish {
  def props(topicPath: TopicPath, message: String, ctx: RequestContext, topicRepo: ActorRef) = Props(classOf[Publish], topicPath, message, ctx, topicRepo).withDispatcher("requests-dispatcher")
}