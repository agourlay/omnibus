package omnibus.service.streamed

import akka.actor.{ ActorRef, Props, PoisonPill }

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Failure

import omnibus.domain.topic._
import omnibus.domain.topic.TopicProtocol._

class StreamTopicView(topicPath: TopicPath, topicRepo: ActorRef) extends StreamedService {

  implicit val executionContext = context.dispatcher

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = {
    case TopicPathRef(path, topicRef) ⇒
      topicRef match {
        case Some(ref) ⇒
          context.system.scheduler.schedule(1.second, 1.second, ref, View)
          context.become(handleStream)
        case None ⇒
          context.parent ! Failure(new TopicNotFoundException(path.prettyStr))
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
  def props(topicPath: TopicPath, topicRepo: ActorRef) = Props(classOf[StreamTopicView], topicPath, topicRepo)
}