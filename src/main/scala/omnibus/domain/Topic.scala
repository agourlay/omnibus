package omnibus.domain

import akka.actor._
import akka.persistence._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection.mutable.ListBuffer

import omnibus.domain.TopicProtocol._

class Topic(val topic: String) extends EventsourcedProcessor with ActorLogging {

  implicit def executionContext = context.dispatcher

  var state = TopicState()
  def updateState(msg: Message): Unit = state = state.update(msg)
  def numEvents = state.size

  var subscribers: Set[ActorRef] = Set.empty[ActorRef]
  var subTopics: Map[String, ActorRef] = Map.empty[String, ActorRef]

  override def preStart() = {
    val myPath = self.path
    log.info(s"Creating new topic $myPath")
    super.preStart()
  }

  val receiveReplay: Receive = {
    case PublishMessage(message)                => updateState(message)
    case SnapshotOffer(_, snapshot: TopicState) => state = snapshot
  }

  val receiveCommand: Receive = {
    case PublishMessage(message)           => publishMessage(message)
    case Subscribe(subscriber)             => subscribe(subscriber)
    case Unsubscribe(subscriber)           => unsubscribe(subscriber)
    case SubscriberNumber                  => sender ! subscribers.size.toString
    case CreateSubTopic(topics)            => createSubTopic(topics)
    case Terminated(refSub)                => subscribers -= refSub
    case Replay(refSub)                    => replayHistory(refSub)
    case Last(refSub)                      => lastMessage(refSub)
    case SinceID(refSub, eventID)          => allMessagesSinceID(refSub, eventID)
    case SinceTS(refSub, timestamp)        => allMessagesSinceTS(refSub, timestamp)
    case BetweenID(refSub, startId, endID) => allMessagesBetweenID(refSub, startId, endID)
    case BetweenTS(refSub, startTs, endTs) => allMessagesBetweenTS(refSub, startTs, endTs)
  }

  def publishMessage(message: Message) = {
    persist(message) { m => updateState(m) }
    // push to subscribers
    subscribers.foreach { actorRef => actorRef ! message }
    // forward message to sub-topics
    subTopics.values foreach { subTopic ⇒ subTopic ! TopicProtocol.PublishMessage(message) }
  }

  def subscribe(subscriber: ActorRef) = {
    context.watch(subscriber)
    subscribers += subscriber
    subscriber ! SubscriberProtocol.AcknowledgeSub(self)
    // subscribe to all subtopics by default
    subTopics.values foreach { subTopic ⇒ subTopic ! TopicProtocol.Subscribe(subscriber) }
  }

  def unsubscribe(subscriber: ActorRef) = {
    context.unwatch(subscriber)
    subscribers -= subscriber
    subscriber ! SubscriberProtocol.AcknowledgeUnsub(self)
  }

  def createSubTopic(topics: List[String]) = topics match {
    case head :: tail => createTopicAndForward(head, tail)
    case _ => log.debug("No more sub topic to create")
  }

  def createTopicAndForward(subTopic: String, topics: List[String]) = {
    log.debug(s"Create sub topic $subTopic and forward $topics")
    if (subTopics.contains(subTopic)) {
      log.debug(s"sub topic $subTopic already exists, forward to its sub topics")
      subTopics(subTopic) ! TopicProtocol.CreateSubTopic(topics)
    } else {
      val subTopicActor = context.actorOf(Props(classOf[Topic], subTopic), subTopic)
      subTopics += (subTopic -> subTopicActor)
      subTopicActor ! TopicProtocol.CreateSubTopic(topics)
    }
  }

  def replayHistory(refSub: ActorRef) = {
    if (state.events.nonEmpty) state.events foreach { message => refSub ! message }
    subTopics.values foreach { subTopic ⇒ subTopic ! TopicProtocol.Replay(refSub) }
  }

  def lastMessage(refSub: ActorRef) = {
    if (state.events.nonEmpty) refSub ! state.events.head
    subTopics.values foreach { subTopic ⇒ subTopic ! TopicProtocol.Last(refSub) }
  }

  def allMessagesSinceID(refSub: ActorRef, eventID: Long) = {
    state.events.filter(_.id > eventID)
      .foreach { message => refSub ! message }
    subTopics.values foreach { subTopic ⇒ subTopic ! TopicProtocol.SinceID(refSub, eventID) }
  }

  def allMessagesSinceTS(refSub: ActorRef, timestamp: Long) = {
    state.events.filter(_.timestamp > timestamp)
      .foreach { message => refSub ! message }
    subTopics.values foreach { subTopic ⇒ subTopic ! TopicProtocol.SinceTS(refSub, timestamp) }
  }

  def allMessagesBetweenID(refSub: ActorRef, startId: Long, endId: Long) = {
    state.events.filter(message => message.id >= startId && message.id <= endId)
      .foreach { message => refSub ! message }
    subTopics.values foreach { subTopic ⇒ subTopic ! TopicProtocol.BetweenID(refSub, startId, endId) }
  }

  def allMessagesBetweenTS(refSub: ActorRef, startTs: Long, endTs: Long) = {
    state.events.filter(message => message.timestamp >= startTs && message.timestamp <= endTs)
      .foreach { message => refSub ! message }
    subTopics.values foreach { subTopic ⇒ subTopic ! TopicProtocol.BetweenTS(refSub, startTs, endTs) }
  }
}

object TopicProtocol {
  case class PublishMessage(message: Message)
  case class Subscribe(subscriber: ActorRef)
  case class Unsubscribe(subscriber: ActorRef)
  case class Replay(subscriber: ActorRef)
  case class Last(subscriber: ActorRef)
  case class SinceID(subscriber: ActorRef, eventId: Long)
  case class SinceTS(subscriber: ActorRef, timestamp: Long)
  case class BetweenID(subscriber: ActorRef, startId: Long, endID: Long)
  case class BetweenTS(subscriber: ActorRef, startTs: Long, endTs: Long)
  case class CreateSubTopic(topics: List[String])
  case object SubscriberNumber
}