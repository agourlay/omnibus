package omnibus.domain.topic

import akka.actor._
import akka.pattern._
import akka.persistence._

import scala.concurrent.duration._
import scala.concurrent._
import scala.concurrent.Promise._
import scala.language.postfixOps
import scala.collection.mutable.ListBuffer

import omnibus.configuration._
import omnibus.domain._
import omnibus.domain.PropagationDirection._
import omnibus.domain.topic.TopicProtocol._
import omnibus.domain.topic.TopicStatProtocol._
import omnibus.domain.subscriber._
import omnibus.domain.subscriber.ReactiveCmd
import omnibus.domain.subscriber.ReactiveMode

class Topic(val topic: String) extends EventsourcedProcessor with ActorLogging {

  implicit val system = context.system
  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(context.system).Timeout.Ask)

  var state = TopicState()
  def updateState(msg: MessageTopic): Unit = {state = state.update(msg)}
  def numEvents = state.size

  val retentionTime = Settings(system).Topic.RetentionTime

  var subscribers: Set[ActorRef] = Set.empty[ActorRef]
  var subTopics: Map[String, ActorRef] = Map.empty[String, ActorRef]

  val statHolder = context.actorOf(TopicStatistics.props(self), "internal-topic-stats")
  val creationDate = System.currentTimeMillis / 1000L
  val topicPath = TopicPath(self)

  val cb = new CircuitBreaker(context.system.scheduler,
      maxFailures = 5,
      callTimeout = timeout.duration,
      resetTimeout = timeout.duration * 10).onOpen(log.warning("CircuitBreaker is now open"))
                                           .onClose(log.warning("CircuitBreaker is now closed"))
                                           .onHalfOpen(log.warning("CircuitBreaker is now half-open"))

  override def preStart() = {
    val myPath = self.path
    log.debug(s"Creating new topic $myPath")
    system.scheduler.schedule(retentionTime, retentionTime, self, TopicProtocol.PurgeTopicData)
    super.preStart()
  }

  val receiveRecover: Receive = {
    case m @ MessageTopic(_, msg)               => updateState(m)
    case SnapshotOffer(_, snapshot: TopicState) => state = snapshot
  }

  val receiveCommand: Receive = {
    case PublishMessage(message)             => cb.withSyncCircuitBreaker(publishMessage(message, sender))
    case ForwardToSubscribers(message)       => sendToSubscribers(message)
    case Subscribe(subscriber)               => subscribe(subscriber)
    case Unsubscribe(subscriber)             => unsubscribe(subscriber)
    case CreateSubTopic(topics, replyTo)     => createSubTopic(topics, replyTo)
    case Terminated(refSub)                  => handleTerminated(refSub)
    case Delete                              => deleteTopic()
    case Leaves(replyTo)                     => leaves(replyTo)
    case View                                => sender ! view()
    case PurgeTopicData                      => purgeOldData()

    case Replay(refSub)                      => forwardMessagesReplay(refSub)
    case Last(refSub)                        => forwardLastMessage(refSub)
    case SinceID(refSub, eventID)            => forwardMessagesSinceID(refSub, eventID)
    case SinceTS(refSub, timestamp)          => forwardMessagesSinceTS(refSub, timestamp)
    case BetweenID(refSub, startId, endID)   => forwardMessagesBetweenID(refSub, startId, endID)
    case BetweenTS(refSub, startTs, endTs)   => forwardMessagesBetweenTS(refSub, startTs, endTs)
    case SetupReactiveMode(refSub, reactCmd) => setupReactiveMode(refSub, reactCmd)

    case Propagation(operation, direction)   => handlePropagation(operation, direction)

    case m @ TopicStatProtocol.PastStats     => statHolder forward m
    case m @ TopicStatProtocol.LiveStats     => statHolder forward m
  }

  def view() : TopicView = {
    val prettyPath = TopicPath.prettyStr(self)
    val subTopicNumber = subTopics.size
    val prettyChildren = subTopics.values.map(TopicPath.prettyStr(_)).toSeq
    TopicView(prettyPath, subTopicNumber, prettyChildren, subscribers.size, numEvents, creationDate)
  }

  def purgeOldData() {
    val timeLimit = System.currentTimeMillis - retentionTime.toMillis
    val limitEvt = state.events.find(_.msg.timestamp < timeLimit)
    limitEvt match {
      case None      =>  log.debug(s"Nothing to purge yet in topic")
      case Some(evt) =>  {
        deleteMessages(evt.seqNumber, true)
        state = TopicState(state.events.filterNot(_.msg.timestamp < timeLimit))
      }
    }                   
  }

  def leaves(replyTo : ActorRef) {
    if (subTopics.isEmpty) replyTo ! view()
    else for(sub <- subTopics.values) sub ! TopicProtocol.Leaves(replyTo)
  }  

  def deleteTopic() {
    if (!state.events.isEmpty){
      val lastIdSeen = state.events.head.seqNumber
      // erase all data from storage
      deleteMessages(lastIdSeen, true)
    }
    self ! PoisonPill
  }

  def handlePropagation(operation: Operation, direction: PropagationDirection) = {
    propagateToDirection(operation, direction)
    self ! operation
  }

  def applyReactiveCmd(refSub: ActorRef, cmd : ReactiveCmd) = {
    cmd.react match {
      case ReactiveMode.REPLAY     => forwardMessagesReplay(refSub)
      case ReactiveMode.LAST       => forwardLastMessage(refSub)
      case ReactiveMode.SINCE_ID   => forwardMessagesSinceID(refSub, cmd.since.get)
      case ReactiveMode.SINCE_TS   => forwardMessagesSinceTS(refSub, cmd.since.get)
      case ReactiveMode.BETWEEN_ID => forwardMessagesBetweenID(refSub, cmd.since.get, cmd.to.get)
      case ReactiveMode.BETWEEN_TS => forwardMessagesBetweenTS(refSub, cmd.since.get, cmd.to.get)
      case ReactiveMode.SIMPLE     => log.debug("simple subscription")
    }
  }

  def propagateToDirection(operation: Operation, direction: PropagationDirection) = direction match {
    case PropagationDirection.UP   => propagateToParent(operation)
    case PropagationDirection.DOWN => propagateToSubTopics(operation)
  }

  def propagateToParent(operation: Operation) = {
    context.parent ! TopicProtocol.Propagation(operation, PropagationDirection.UP)
  }

  def propagateToSubTopics(operation: Operation) = {
    subTopics.values foreach { subTopic ⇒ subTopic ! TopicProtocol.Propagation(operation, PropagationDirection.DOWN) }
  }

  def setupReactiveMode(refSub: ActorRef, cmd : ReactiveCmd) = {
    applyReactiveCmd(refSub, cmd)
    val reactiveMessageToFW = cmd.react match {
      case ReactiveMode.REPLAY     => TopicProtocol.Replay(refSub)
      case ReactiveMode.LAST       => TopicProtocol.Last(refSub)
      case ReactiveMode.SINCE_ID   => TopicProtocol.SinceID(refSub, cmd.since.get)
      case ReactiveMode.SINCE_TS   => TopicProtocol.SinceTS(refSub, cmd.since.get)
      case ReactiveMode.BETWEEN_ID => TopicProtocol.BetweenTS(refSub, cmd.since.get, cmd.to.get)
      case ReactiveMode.BETWEEN_TS => TopicProtocol.BetweenTS(refSub, cmd.since.get, cmd.to.get)
    }
    // forward reactive command to children
    propagateToDirection(reactiveMessageToFW, PropagationDirection.DOWN)
  }

  def publishMessage(message: String, replyTo : ActorRef) = {
    // persist in topic state
    val event = Message(lastSequenceNr + 1, topicPath, message)
    persist(MessageTopic(event.id, event)) { evt => 
      updateState(evt) 
      // push to subscribers
      sendToSubscribers(evt.msg)
      // forward message to parent for ancestor visibility
      propagateToDirection(ForwardToSubscribers(evt.msg), PropagationDirection.UP)
      replyTo ! TopicProtocol.MessagePublished
    }
  }

  def sendToSubscribers(message: Message) = {
    subscribers.foreach { actorRef => actorRef ! message }  // push to subscribers
    statHolder ! TopicStatProtocol.MessageReceived  // report stats
  }

  def subscribe(subscriber: ActorRef) = {
    if (!subscribers.contains(subscriber)){
      context.watch(subscriber)
      subscribers += subscriber
      subscriber ! SubscriberProtocol.AcknowledgeSub(self)
      statHolder ! TopicStatProtocol.SubscriberAdded       // report stats
    }
  }

  def unsubscribe(subscriber: ActorRef) = {
    context.unwatch(subscriber)
    subscribers -= subscriber
    subscriber ! SubscriberProtocol.AcknowledgeUnsub(self)
    statHolder ! TopicStatProtocol.SubscriberRemoved    // report stats
  }

  // It is either a subtopic or a subscriber
  def handleTerminated(ref : ActorRef) = {
    // FIXME it is SUPER ugly there
    if (subTopics.values.toSeq.contains(ref)) {
      val key = subTopics.find(_._2 == ref).get._1
      subTopics -= (key)
    } else {
      unsubscribe(ref)
    }
  }

  def createSubTopic(topics: List[String], replyTo : ActorRef) = topics match {
    case head :: tail => createTopicAndForward(head, tail, replyTo)
    case _            => replyTo ! TopicProtocol.TopicCreated(self)
  }

  def createTopicAndForward(subTopic: String, topics: List[String], replyTo : ActorRef) = {
    log.debug(s"Create sub topic $subTopic and forward $topics")
    if (subTopics.contains(subTopic)) {
      log.debug(s"sub topic $subTopic already exists, forward to its sub topics")
      subTopics(subTopic) ! TopicProtocol.CreateSubTopic(topics, replyTo)
    } else {
      val subTopicActor = context.actorOf(Topic.props(subTopic), subTopic)
      context.watch(subTopicActor)
      subTopics += (subTopic -> subTopicActor)
      subTopicActor ! TopicProtocol.CreateSubTopic(topics, replyTo)
      statHolder ! TopicStatProtocol.SubTopicAdded     // report stats
    }
  }

  def forwardMessagesReplay(refSub: ActorRef) = {
    if (state.events.nonEmpty) state.events.reverse.foreach { evt => refSub ! evt.msg }
  }  

  def forwardLastMessage(refSub: ActorRef) = {
    if (state.events.nonEmpty) refSub ! state.events.head.msg
  }  

  def forwardMessagesSinceID(refSub: ActorRef, eventID: Long) = {
    state.events.reverse.filter(_.msg.id > eventID)
                        .foreach { evt => refSub ! evt.msg }
  }

  def forwardMessagesSinceTS(refSub: ActorRef, timestamp: Long) = {
    state.events.reverse.filter(_.msg.timestamp > timestamp)
                        .foreach { evt => refSub ! evt.msg }
  }

  def forwardMessagesBetweenID(refSub: ActorRef, startId: Long, endId: Long) = {
    state.events.reverse.filter(evt => evt.msg.id >= startId && evt.msg.id <= endId)
                        .foreach { evt => refSub ! evt.msg }
  }

  def forwardMessagesBetweenTS(refSub: ActorRef, startTs: Long, endTs: Long) = {
    state.events.reverse.filter(evt => evt.msg.timestamp >= startTs && evt.msg.timestamp <= endTs)
                        .foreach { evt => refSub ! evt.msg }
  }
}

object TopicProtocol {
  case class PublishMessage(message: String) extends Operation
  case class ForwardToSubscribers(message: Message) extends Operation
  case class SetupReactiveMode(subscriber: ActorRef, cmd : ReactiveCmd) extends Operation
  case class Subscribe(subscriber: ActorRef) extends Operation
  case class Unsubscribe(subscriber: ActorRef) extends Operation
  case class CreateSubTopic(topics: List[String], replyTo : ActorRef)
  case class Leaves(replyTo : ActorRef)
  case class TopicCreated(topicRef : ActorRef)
  case object SubscriberNumber
  case object Delete
  case object View
  case object PurgeTopicData
  case object MessagePublished

  // ReactiveCmd operations
  case class Replay(subscriber: ActorRef) extends Operation
  case class Last(subscriber: ActorRef) extends Operation
  case class SinceID(subscriber: ActorRef, eventId: Long) extends Operation
  case class SinceTS(subscriber: ActorRef, timestamp: Long) extends Operation
  case class BetweenID(subscriber: ActorRef, startId: Long, endID: Long) extends Operation
  case class BetweenTS(subscriber: ActorRef, startTs: Long, endTs: Long) extends Operation

  // container used to propagate operation in topic tree
  case class Propagation(message: TopicProtocol.Operation, direction: PropagationDirection)
  trait Operation{}
}

object Topic {  
  def props(topic: String) = Props(classOf[Topic], topic).withDispatcher("topics-dispatcher")
}