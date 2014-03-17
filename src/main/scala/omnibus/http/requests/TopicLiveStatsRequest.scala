package omnibus.http.request

import akka.actor._

import spray.httpx.SprayJsonSupport._
import spray.routing._

import omnibus.http.JsonSupport._
import omnibus.domain.topic._
import omnibus.repository._

class TopicLiveStatsRequest(topicPath: TopicPath, ctx : RequestContext, topicRepo: ActorRef) extends RestRequest(ctx) {

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = waitingLookup orElse handleTimeout

  def waitingTopicLiveStats : Receive = {
    case ts : TopicStatisticValue  => {
      ctx.complete (ts)
      self ! PoisonPill
    }
  }

  def waitingLookup : Receive = {
    case TopicPathRef(topicPath, optRef) => handleTopicPathRef(topicPath, optRef)
  }

  def handleTopicPathRef(topicPath: TopicPath, topicRef : Option[ActorRef]) = topicRef match {
    case Some(ref) => {
      ref ! TopicStatProtocol.LiveStats
      context.become(waitingTopicLiveStats orElse handleTimeout)
    }
    case None      => {
      ctx.complete(new TopicNotFoundException(topicPath.prettyStr))
      self ! PoisonPill
    }  
  }
}

object TopicLiveStatsRequest {
   def props(topicPath: TopicPath, ctx : RequestContext, topicRepo: ActorRef) 
     = Props(classOf[TopicLiveStatsRequest], topicPath, ctx, topicRepo).withDispatcher("requests-dispatcher")
}