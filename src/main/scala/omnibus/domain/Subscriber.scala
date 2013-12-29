package omnibus.domain

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps

import omnibus.domain.SubscriberProtocol._
import omnibus.domain.ReactiveMode._

class Subscriber(val responder: ActorRef, val topics: Set[ActorRef], val reactiveCmd: ReactiveCmd)
    extends Actor with ActorLogging {

  implicit val system = context.system
  implicit def executionContext = context.dispatcher

  var pendingTopic: Set[ActorRef] = topics
  var topicListened: Set[ActorRef] = Set.empty[ActorRef]
  // set of all event ids seen by this subscriber 
  var idsSeen: Set[Long] = Set.empty[Long]

  override def preStart() = {
    val prettyTopics = prettySubscription(topics)
    val mode = reactiveCmd.mode
    log.info(s"Creating sub on topics $prettyTopics with mode $mode")

    // subscribe to every topic
    for (topic <- topics) { topic ! TopicProtocol.Subscribe(self) }

    // schedule pending retry every minute
    system.scheduler.schedule(1 minute, 1 minute, self, SubscriberProtocol.RefreshTopics)
  }

  def receive = {
    case AcknowledgeSub(topicRef)   => ackSubscription(topicRef)
    case AcknowledgeUnsub(topicRef) => topicListened -= topicRef
    case StopSubscription           => self ! PoisonPill
    case PendingAckTopic            => sender ! prettySubscription(pendingTopic)
    case AcknowledgedTopic          => sender ! prettySubscription(topicListened)
    case RefreshTopics              => refreshTopics()
    case Terminated(topicRef)       => handleTopicDeath(topicRef)
    case message: Message           => sendMessage(message)
  }

  def formatMessagePayload(msg: Message): Any = msg

  def notYetPlayed(msg: Message): Boolean = !idsSeen.contains(msg.id)

  def filterAccordingMode(msg: Message) = reactiveCmd.mode match {
    case ReactiveMode.BETWEEN_ID => msg.id >= reactiveCmd.since.get && msg.id <= reactiveCmd.to.get
    case ReactiveMode.BETWEEN_TS => msg.timestamp >= reactiveCmd.since.get && msg.timestamp <= reactiveCmd.to.get
    case _ => true
  }

  def sendMessage(msg: Message) = {
    // An event can only be played once by subscription
    if (notYetPlayed(msg) && filterAccordingMode(msg)) {
      responder ! formatMessagePayload(msg)
      idsSeen += msg.id
    }
  }

  def handleTopicDeath(topicRef: ActorRef) = {
    topicListened -= topicRef
    pendingTopic += topicRef
  }

  def refreshTopics() {
    log.info(s"Refresh sub in $topicListened")
    for (topic <- topicListened) { topic ! TopicProtocol.Subscribe(self) }

    log.info(s"Retry pending sub in $pendingTopic")
    for (topic <- pendingTopic) { topic ! TopicProtocol.Subscribe(self) }
  }

  def ackSubscription(topicRef: ActorRef) = {
    topicListened += topicRef
    pendingTopic -= topicRef
    context.watch(topicRef)
    log.debug(s"subscriber successfully subscribed to $topicRef")
    // we are successfully registered to the topic, let's use the reactive cm if not simple
    if (reactiveCmd.mode != ReactiveMode.SIMPLE){
      topicRef ! TopicProtocol.SetupReactiveMode(self, reactiveCmd)
    }
  }

  def prettySubscription(topicToDisplay: Set[ActorRef]): String = {
    val setOfTopic = topicToDisplay.map(_.path)
      .map(_.toString)
      .map(_.split("/topic-repository").toList(1))
    setOfTopic.mkString(" + ")
  }
}

object SubscriberProtocol {
  case class AcknowledgeSub(topic: ActorRef)
  case class AcknowledgeUnsub(topic: ActorRef)
  case object StopSubscription
  case object PendingAckTopic
  case object AcknowledgedTopic
  case object RefreshTopics
}