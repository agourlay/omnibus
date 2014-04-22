package omnibus.api.request

import akka.actor._

import spray.routing._
import spray.http._

import omnibus.domain.topic._
import omnibus.domain.topic.TopicRepositoryProtocol._

class DeleteTopic(topicPath: TopicPath, ctx : RequestContext, topicRepo: ActorRef) extends RestRequest(ctx) {

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = waitingLookup orElse handleTimeout

  def waitingAck : Receive = {
    case TopicDeletedFromRepo(topicPath) => {
      val prettyTopic = topicPath.prettyStr()
      ctx.complete(StatusCodes.Accepted, s"Topic $prettyTopic deleted\n")
      self ! PoisonPill
    } 
  }

  def waitingLookup : Receive = {
    case TopicPathRef(topicPath, optRef) => handleTopicPathRef(topicPath, optRef)
  }

  def handleTopicPathRef(topicPath: TopicPath, topicRef : Option[ActorRef]) = topicRef match {
    case Some(ref) => {
      ref ! TopicProtocol.Delete
      topicRepo ! TopicRepositoryProtocol.DeleteTopic(topicPath)
      context.become(waitingAck orElse handleTimeout)
    }
    case None      => {
      ctx.complete(new TopicNotFoundException(topicPath.prettyStr))
      self ! PoisonPill
    }  
  }
}

object DeleteTopic {
   def props(topicPath: TopicPath, ctx : RequestContext, topicRepo: ActorRef) 
     = Props(classOf[DeleteTopic], topicPath, ctx, topicRepo).withDispatcher("requests-dispatcher")
}