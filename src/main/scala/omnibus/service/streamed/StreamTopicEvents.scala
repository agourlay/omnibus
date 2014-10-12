package omnibus.service.streamed

import akka.actor.{ Actor, ActorRef, Props }

import scala.util.Failure

import omnibus.domain.topic._
import omnibus.domain.subscriber._
import omnibus.domain.subscriber.SubscriberRepositoryProtocol._
import omnibus.domain.subscriber.SubscriberSupport._

class StreamTopicEvent(replyTo: ActorRef, sd: SubscriptionDescription, subRepo: ActorRef, topicRepo: ActorRef) extends StreamedService(replyTo) {

  var pendingTopics = Set.empty[TopicPath]
  var validTopics = Set.empty[ActorRef]

  val topics = TopicPath.multi(sd.topicPath.prettyStr)
  topics foreach { topic ⇒
    pendingTopics += topic
    topicRepo ! TopicRepositoryProtocol.LookupTopic(topic)
  }

  override def receive = {
    case TopicPathRef(topicPath, topicRef) ⇒
      topicRef match {
        case None ⇒ replyTo ! Failure(new TopicNotFoundException(topicPath.prettyStr))
        case Some(ref) ⇒
          validTopics += ref
          if (validTopics.size == pendingTopics.size) {
            subRepo ! SubscriberRepositoryProtocol.CreateSub(validTopics, replyTo, sd.reactiveCmd, sd.ip, sd.subSupport)
          }
      }
  }
}

object StreamTopicEvent {
  def props(replyTo: ActorRef, sd: SubscriptionDescription, subRepo: ActorRef, topicRepo: ActorRef) =
    Props(classOf[StreamTopicEvent], replyTo, sd, subRepo, topicRepo).withDispatcher("requests-dispatcher")
}