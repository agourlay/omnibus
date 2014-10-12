package omnibus.service.streamed

import akka.actor.{ Actor, ActorRef, Props, PoisonPill }

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Failure

import omnibus.domain.topic._
import omnibus.domain.topic.TopicRepositoryProtocol._
import omnibus.domain.topic.TopicProtocol._
import omnibus.configuration._
import omnibus.api.streaming.sse.ServerSentEventResponse
import omnibus.api.streaming.sse.ServerSentEventSupport._

class StreamTopicView(replyTo: ActorRef, topicPath: TopicPath, topicRepo: ActorRef) extends StreamedService(replyTo) {

  implicit def executionContext = context.dispatcher

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = {
    case TopicPathRef(topicPath, topicRef) ⇒
      topicRef match {
        case Some(topicRef) ⇒
          context.system.scheduler.schedule(1.second, 1.second, topicRef, View)
          context.become(handleStream)
        case None ⇒
          replyTo ! Failure(new TopicNotFoundException(topicPath.prettyStr))
          self ! PoisonPill
      }
  }

  def handleStream: Receive = {
    case topicView: TopicView ⇒ replyTo ! topicView
  }
}

object HttpTopicStatProtocol {
  object RequestTopicStats
}

object StreamTopicView {
  def props(replyTo: ActorRef, topicPath: TopicPath, topicRepo: ActorRef) = Props(classOf[StreamTopicView], replyTo, topicPath, topicRepo).withDispatcher("streaming-dispatcher")
}