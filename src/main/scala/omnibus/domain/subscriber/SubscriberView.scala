package omnibus.domain.subscriber

import akka.actor.ActorRef
import omnibus.domain.topic.TopicPath
import SubscriberSupport._

case class SubscriberView(ref: ActorRef, id: String, topic: String, ip: String, mode: String, support: String, creationDate: Long = System.currentTimeMillis / 1000)

case class SubscriptionDescription(topicPath: TopicPath, reactiveCmd: ReactiveCmd, ip: String, subSupport: SubscriberSupport)