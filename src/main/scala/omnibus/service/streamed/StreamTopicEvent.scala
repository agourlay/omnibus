package omnibus.service.streamed

import akka.actor.{ ActorRef, Props }

import scala.util.Failure

import omnibus.domain.topic._
import omnibus.domain.subscriber._

class StreamTopicEvent(sd: SubscriptionDescription, subRepo: ActorRef, topicRepo: ActorRef) extends StreamedService {

  val pendingTopics = scala.collection.mutable.Set.empty[TopicPath]
  val validTopics = scala.collection.mutable.Set.empty[ActorRef]

  val topics = TopicPath.multi(sd.topicPath.prettyStr)
  topics foreach { topic ⇒
    pendingTopics += topic
    topicRepo ! TopicRepositoryProtocol.LookupTopic(topic)
  }

  override def receive = {
    case TopicPathRef(topicPath, topicRef) ⇒
      topicRef match {
        case None ⇒ context.parent ! Failure(new TopicNotFoundException(topicPath.prettyStr))
        case Some(ref) ⇒
          validTopics += ref
          if (validTopics.size == pendingTopics.size) {
            subRepo ! SubscriberRepositoryProtocol.CreateSub(validTopics.toSet, context.parent, sd.reactiveCmd, sd.ip, sd.subSupport)
          }
      }
  }
}

object StreamTopicEvent {
  def props(sd: SubscriptionDescription, subRepo: ActorRef, topicRepo: ActorRef) =
    Props(classOf[StreamTopicEvent], sd, subRepo, topicRepo)
}