package omnibus.domain.subscriber

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps

import omnibus.domain._
import omnibus.domain.topic._
import omnibus.domain.subscriber.SubscriberProtocol._
import omnibus.domain.subscriber.ReactiveMode._

class Subscriber(val responder: ActorRef, val topics: Set[ActorRef], val reactiveCmd: ReactiveCmd, val timestamp: Long)
    extends Actor with ActorLogging {

  implicit val system = context.system
  implicit def executionContext = context.dispatcher

  var pendingTopic: Set[ActorRef] = topics
  var topicListened: Set[ActorRef] = Set.empty[ActorRef]
  var idsSeen: Set[Long] = Set.empty[Long]   // set of all event ids seen by this subscriber 
  val topicsPath : Set[TopicPath] = topics.map(TopicPath(_))

  override def preStart() = {
    val prettyTopics = TopicPath.prettySubscription(topics)
    val react = reactiveCmd.react
    val sub = reactiveCmd.sub
    log.debug(s"Creating sub on topics $prettyTopics with react $react and sub $sub")

    // Death pact with responder! 
    context.watch(responder)

    // subscribe to every topic
    for (topic <- topics) { topic ! TopicProtocol.Subscribe(self) }

    // schedule pending retry every minute
    system.scheduler.schedule(1 minute, 1 minute, self, SubscriberProtocol.RefreshTopics)
  }

  def receive = {
    case AcknowledgeSub(topicRef)   => ackSubscription(topicRef)
    case AcknowledgeUnsub(topicRef) => topicListened -= topicRef
    case StopSubscription           => stopSubscription()
    case RefreshTopics              => refreshTopics()
    case message: Message           => sendMessage(message)
  }

  def stopSubscription() {
    log.debug(s"End of subscriber $self")
    self ! PoisonPill
  }

  def notYetPlayed(msg: Message): Boolean = !idsSeen.contains(msg.id)

  def filterAccordingReactMode(msg: Message) = reactiveCmd.react match {
    case ReactiveMode.BETWEEN_ID => msg.id >= reactiveCmd.since.get && msg.id <= reactiveCmd.to.get
    case ReactiveMode.BETWEEN_TS => msg.timestamp >= reactiveCmd.since.get && msg.timestamp <= reactiveCmd.to.get
    case _ => true
  }

  def filterAccordingSubMode(msg: Message) = reactiveCmd.sub match {
    case SubscriptionMode.WIDE => true
    case SubscriptionMode.CLASSIC => topicsPath.contains(msg.topicPath)
  }

  def sendMessage(msg: Message) = {
    // An event can only be played once by subscription
    if (notYetPlayed(msg) && filterAccordingReactMode(msg) && filterAccordingSubMode(msg)) {
      responder ! msg
      idsSeen += msg.id
    }
  }

  def refreshTopics() {
    log.debug(s"Refresh sub in $topicListened")
    for (topic <- topicListened) { topic ! TopicProtocol.Subscribe(self) }

    log.debug(s"Retry pending sub in $pendingTopic")
    for (topic <- pendingTopic) { topic ! TopicProtocol.Subscribe(self) }
  }

  def ackSubscription(topicRef: ActorRef) = {
    topicListened += topicRef
    pendingTopic -= topicRef
    // Death pact with topic ! 
    context.watch(topicRef)
    log.debug(s"subscriber successfully subscribed to $topicRef")
    // we are successfully registered to the topic, let's use the reactive cm if not simple
    reactiveCmd.react match {
      case ReactiveMode.SIMPLE => log.debug("no reactive mode for simple")
      case _                   => topicRef ! TopicProtocol.SetupReactiveMode(self, reactiveCmd)
    }
  }
}

object SubscriberProtocol {
  case class AcknowledgeSub(topic: ActorRef)
  case class AcknowledgeUnsub(topic: ActorRef)
  case object StopSubscription
  case object RefreshTopics
}

object Subscriber {
  def props(responder: ActorRef, topics: Set[ActorRef], reactiveCmd: ReactiveCmd) 
    = Props(classOf[Subscriber], responder, topics, reactiveCmd, System.currentTimeMillis / 1000).withDispatcher("subscribers-dispatcher")
}