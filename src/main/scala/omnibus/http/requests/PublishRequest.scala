package omnibus.http.request

import akka.actor._

import spray.routing._
import spray.http._

import omnibus.domain.topic._
import omnibus.repository._

class PublishRequest(topicPath: TopicPath, message: String, ctx : RequestContext, topicRepo: ActorRef) extends RestRequest(ctx) {

  val prettyTopic = topicPath.prettyStr()

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = waitingLookup orElse handleTimeout

  def waitingAck : Receive = {
    case TopicProtocol.MessagePublished  => {
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

object PublishRequest {
   def props(topicPath: TopicPath, message: String, ctx : RequestContext, topicRepo: ActorRef) 
     = Props(classOf[PublishRequest], topicPath, message, ctx, topicRepo).withDispatcher("requests-dispatcher")
}