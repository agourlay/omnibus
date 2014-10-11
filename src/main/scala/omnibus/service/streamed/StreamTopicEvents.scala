package omnibus.service.streamed

import akka.actor.{ Actor, ActorRef, Props }

import spray.routing._

import scala.util.Failure

import omnibus.domain.topic._
import omnibus.domain.subscriber._
import omnibus.domain.subscriber.SubscriberRepositoryProtocol._
import omnibus.domain.subscriber.SubscriberSupport._

class StreamTopicEvent(replyTo: ActorRef, topicPath: TopicPath, reactiveCmd: ReactiveCmd, ip: String, subSupport: SubscriberSupport, subRepo: ActorRef, topicRepo: ActorRef) extends StreamedService(replyTo) {

  var pendingTopics = Set.empty[TopicPath]
  var validTopics = Set.empty[ActorRef]

  val topics = TopicPath.multi(topicPath.prettyStr)
  topics foreach { topic ⇒
    pendingTopics += topic
    topicRepo ! TopicRepositoryProtocol.LookupTopic(topic)
  }

  override def receive = {
    case TopicPathRef(topicPath, optRef) ⇒ handleTopicPathRef(topicPath, optRef)
  }

  def handleTopicPathRef(topicPath: TopicPath, topicRef: Option[ActorRef]) = topicRef match {
    case None ⇒ replyTo ! Failure(new TopicNotFoundException(topicPath.prettyStr))
    case Some(ref) ⇒
      validTopics += ref
      if (validTopics.size == pendingTopics.size) {
        subRepo ! SubscriberRepositoryProtocol.CreateSub(validTopics, replyTo, reactiveCmd, ip, SubscriberSupport.SSE)
      }
  }
}

object StreamTopicEvent {
  def props(replyTo: ActorRef, topicPath: TopicPath, reactiveCmd: ReactiveCmd, ip: String, subSupport: SubscriberSupport, subRepo: ActorRef, topicRepo: ActorRef) =
    Props(classOf[StreamTopicEvent], replyTo, topicPath, reactiveCmd, ip, subSupport, subRepo, topicRepo).withDispatcher("requests-dispatcher")
}