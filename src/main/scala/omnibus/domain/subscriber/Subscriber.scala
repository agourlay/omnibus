package omnibus.domain.subscriber

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps

import omnibus.core.actors.CommonActor
import omnibus.domain.topic._
import omnibus.domain.subscriber.SubscriberProtocol._

class Subscriber(val channel: ActorRef, val topics: Set[ActorRef], val reactiveCmd: ReactiveCmd, val timestamp: Long) extends CommonActor {

  implicit val executionContext = context.dispatcher

  val pendingTopic = scala.collection.mutable.Set.empty[ActorRef] ++ topics
  val topicListened = scala.collection.mutable.Set.empty[ActorRef]
  var pendingScheduler: Cancellable = _

  override def preStart() = {
    val prettyTopics = TopicPath.prettySubscription(topics)
    val react = reactiveCmd.react
    log.debug(s"Creating sub on topics $prettyTopics with react $react")

    context.watch(channel)

    // subscribe to every topic
    for (topic ← topics) { topic ! TopicProtocol.Subscribe(self) }

    // schedule pending retry every minute
    pendingScheduler = context.system.scheduler.schedule(1 minute, 1 minute, self, SubscriberProtocol.RetryPending)
  }

  override def postStop() = {
    channel ! PoisonPill
    pendingScheduler.cancel()
  }

  def receive = {
    case AcknowledgeSub(topicRef)                      ⇒ ackSubscription(topicRef)
    case AcknowledgeUnsub(topicRef)                    ⇒ topicListened -= topicRef
    case StopSubscription                              ⇒ stopSubscription()
    case RetryPending                                  ⇒ retryPending()
    case topicEvent: TopicEvent                        ⇒ channel ! topicEvent
    case Terminated(ref)                               ⇒ stopSubscription()
    case TopicContentProtocol.ProcessorId(processorId) ⇒ setupSubscription(processorId)
    case TopicProtocol.TopicCreated(newTopicRef)       ⇒ topicCreatedWithinSub(newTopicRef)
  }

  def topicCreatedWithinSub(newTopicRef: ActorRef) {
    newTopicRef ! TopicProtocol.ProcessorId(self)
  }

  def setupSubscription(processorId: String) {
    context.actorOf(Subscription.props(processorId, reactiveCmd))
  }

  def stopSubscription() {
    log.debug(s"End of subscriber $self")
    self ! PoisonPill
  }

  def retryPending() {
    for (topic ← pendingTopic) {
      log.debug(s"Retry pending subcription to $topic")
      topic ! TopicProtocol.Subscribe(self)
    }
  }

  def ackSubscription(topicRef: ActorRef) = {
    topicListened += topicRef
    pendingTopic -= topicRef
    context.watch(topicRef)
    log.debug(s"successfully subscribed to $topicRef")
    // retrieve all children processor id
    topicRef ! TopicProtocol.CascadeProcessorId(self)
  }
}

object SubscriberProtocol {
  case class AcknowledgeSub(topic: ActorRef)
  case class AcknowledgeUnsub(topic: ActorRef)
  case class Subscription(topicId: String)
  case object StopSubscription
  case object RetryPending
}

object Subscriber {
  def props(channel: ActorRef, topics: Set[ActorRef], reactiveCmd: ReactiveCmd) = Props(classOf[Subscriber], channel, topics, reactiveCmd, System.currentTimeMillis / 1000).withDispatcher("subscribers-dispatcher")
}