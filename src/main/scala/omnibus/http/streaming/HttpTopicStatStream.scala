package omnibus.http.streaming

import akka.actor._

import spray.routing._
import spray.http._

import scala.language.postfixOps

import omnibus.http.JsonSupport._
import omnibus.domain.topic._
import omnibus.repository._
import omnibus.configuration._

class HttpTopicStatStream(topicPath : TopicPath, ctx : RequestContext, topicRepo : ActorRef) extends StreamingResponse(ctx.responder) {

  implicit def executionContext = context.dispatcher
  implicit def system = context.system

  val sampling = Settings(system).Statistics.Sampling

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def startText = s"~~> Streaming topic statistics\n"

  override def receive = waitingLookup orElse super.receive

  def waitingLookup : Receive = {
    case TopicPathRef(topicPath, optRef) => handleTopicPathRef(topicPath, optRef)
  }

  def handleTopicPathRef(topicPath: TopicPath, topicRef : Option[ActorRef]) = topicRef match {
    case Some(topicRef) => {
      context.system.scheduler.schedule(sampling, sampling){ topicRef ! TopicStatProtocol.LiveStats }
      context.become(handleStream orElse  super.receive)
    }
    case None      => {
      ctx.complete(new TopicNotFoundException(topicPath.prettyStr))
      self ! PoisonPill
    }  
  }

  def handleStream : Receive = {
    case stat : TopicStatisticValue => ctx.responder ! MessageChunk("data: "+ formatTopicStats.write(stat) +"\n\n")
  }
}

object HttpTopicStatStreamProtocol {
  object RequestTopicStats
}

object HttpTopicStatStream {
  def props(topicPath: TopicPath, ctx : RequestContext, topicRepo: ActorRef)
    = Props(classOf[HttpTopicStatStream], topicPath, ctx, topicRepo).withDispatcher("streaming-dispatcher")
}