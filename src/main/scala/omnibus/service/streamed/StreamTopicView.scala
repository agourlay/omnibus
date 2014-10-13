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

class StreamTopicView(topicPath: TopicPath, topicRepo: ActorRef) extends StreamedService {

  implicit def executionContext = context.dispatcher

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = {
    case TopicPathRef(topicPath, topicRef) ⇒
      topicRef match {
        case Some(topicRef) ⇒
          context.system.scheduler.schedule(1.second, 1.second, topicRef, View)
          context.become(handleStream)
        case None ⇒
          context.parent ! Failure(new TopicNotFoundException(topicPath.prettyStr))
          self ! PoisonPill
      }
  }

  def handleStream: Receive = {
    case topicView: TopicView ⇒ context.parent ! topicView
  }
}

object HttpTopicStatProtocol {
  object RequestTopicStats
}

object StreamTopicView {
  def props(topicPath: TopicPath, topicRepo: ActorRef) = Props(classOf[StreamTopicView], topicPath, topicRepo).withDispatcher("streaming-dispatcher")
}