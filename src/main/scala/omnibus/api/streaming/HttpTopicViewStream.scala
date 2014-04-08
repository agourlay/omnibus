package omnibus.api.streaming

import akka.actor._

import spray.routing._
import spray.http._

import scala.concurrent.duration._
import scala.language.postfixOps

import omnibus.api.endpoint.JsonSupport._
import omnibus.domain.topic._
import omnibus.domain.topic.TopicRepositoryProtocol._
import omnibus.domain.topic.TopicProtocol._
import omnibus.configuration._

class HttpTopicViewStream(topicPath : TopicPath, ctx : RequestContext, topicRepo : ActorRef) extends StreamingResponse(ctx.responder) {

  implicit def executionContext = context.dispatcher

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def startText = s"~~> Streaming topic statistics\n"

  override def receive = waitingLookup orElse super.receive

  def waitingLookup : Receive = {
    case TopicPathRef(topicPath, optRef) => handleTopicPathRef(topicPath, optRef)
  }

  def handleTopicPathRef(topicPath: TopicPath, topicRef : Option[ActorRef]) = topicRef match {
    case Some(topicRef) => {
      context.system.scheduler.schedule(1.second, 1.second){ 
        topicRef ! View 
      }
      context.become(handleStream orElse super.receive)
    }
    case None      => {
      ctx.complete(new TopicNotFoundException(topicPath.prettyStr))
      self ! PoisonPill
    }  
  }

  def handleStream : Receive = {
   case topic : TopicView => ctx.responder ! MessageChunk("data: "+ formatTopicView.write(topic) +"\n\n")
  }
}

object HttpTopicStatStreamProtocol {
  object RequestTopicStats
}

object HttpTopicViewStream {
  def props(topicPath: TopicPath, ctx : RequestContext, topicRepo: ActorRef)
    = Props(classOf[HttpTopicViewStream], topicPath, ctx, topicRepo).withDispatcher("streaming-dispatcher")
}