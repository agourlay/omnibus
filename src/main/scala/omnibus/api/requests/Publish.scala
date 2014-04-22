package omnibus.api.request

import akka.actor._

import spray.routing._
import spray.http._

import omnibus.domain.topic._
import omnibus.domain.topic.TopicRepositoryProtocol._

class Publish(topicPath: TopicPath, message: String, ctx : RequestContext, topicRepo: ActorRef) extends RestRequest(ctx) {

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = waitingLookup orElse handleTimeout

  def waitingAck : Receive = {
    case TopicProtocol.MessagePublished  => {
      val prettyTopic = topicPath.prettyStr()
      ctx.complete(StatusCodes.Accepted, s"Message published to topic $prettyTopic\n")
      self ! PoisonPill
    }
  }

  def waitingLookup : Receive = {
    case TopicPathRef(topicPath, optRef) => handleTopicPathRef(topicPath, optRef)
  }

  def handleTopicPathRef(topicPath: TopicPath, topicRef : Option[ActorRef]) = topicRef match {
    case Some(ref) => {
      ref ! TopicProtocol.PublishMessage(message)
      context.become(waitingAck orElse handleTimeout)
    }
    case None      => {
      ctx.complete(new TopicNotFoundException(topicPath.prettyStr))
      self ! PoisonPill
    }  
  }
}

object Publish {
   def props(topicPath: TopicPath, message: String, ctx : RequestContext, topicRepo: ActorRef) 
     = Props(classOf[Publish], topicPath, message, ctx, topicRepo).withDispatcher("requests-dispatcher")
}