package omnibus.domain.topic

import akka.actor._

import omnibus.domain._
import omnibus.domain.PropagationDirection._
import omnibus.domain.topic.TopicProtocol._
import omnibus.domain.subscriber._
import omnibus.domain.subscriber.ReactiveCmd

class Topic(val topic: String) extends Actor with ActorLogging {

  implicit def executionContext = context.dispatcher

  def numEvents = 0L // TODO get from content

  val creationDate = System.currentTimeMillis / 1000L
  val topicPath = TopicPath(self)

  var subscribers: Set[ActorRef] = Set.empty[ActorRef]
  var subTopics: Map[String, ActorRef] = Map.empty[String, ActorRef]

  val statisticsHolder = context.actorOf(TopicStatistics.props(self), "internal-topic-stats")
  val contentHolder = context.actorOf(TopicContent.props(topicPath), "internal-topic-content")

  def receive = {
    case PublishMessage(message)             => contentHolder ! TopicContentProtocol.Publish(message, sender)
    case ForwardToSubscribers(message)       => sendToSubscribers(message)
    case Subscribe(subscriber)               => subscribe(subscriber)
    case Unsubscribe(subscriber)             => unsubscribe(subscriber)
    case CreateSubTopic(topics, replyTo)     => createSubTopic(topics, replyTo)
    case Terminated(refSub)                  => handleTerminated(refSub)
    case Delete                              => deleteTopic()
    case Leaves(replyTo)                     => leaves(replyTo)
    case View                                => sender ! view()
    case SetupReactiveMode(refSub, reactCmd) => setupReactiveMode(refSub, reactCmd)
    case Propagation(operation, direction)   => handlePropagation(operation, direction)

    case TopicContentProtocol.Saved(msg,replyTo) => messageSaved(msg, replyTo)

    case m @ TopicStatProtocol.PastStats     => statisticsHolder forward m
    case m @ TopicStatProtocol.LiveStats     => statisticsHolder forward m
  }

  def view() : TopicView = {
    val prettyPath = TopicPath.prettyStr(self)
    val subTopicNumber = subTopics.size
    val prettyChildren = subTopics.values.map(TopicPath.prettyStr(_)).toSeq
    TopicView(prettyPath, subTopicNumber, prettyChildren, subscribers.size, numEvents, creationDate)
  }

  def leaves(replyTo : ActorRef) {
    if (subTopics.isEmpty) replyTo ! view()
    else for(sub <- subTopics.values) sub ! TopicProtocol.Leaves(replyTo)
  }  

  def deleteTopic() {
    contentHolder ! TopicContentProtocol.DeleteContent
    self ! PoisonPill
  }

  def handlePropagation(operation: Operation, direction: PropagationDirection) = {
    propagateToDirection(operation, direction)
    self ! operation
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
    // forward reactive command to children
    propagateToDirection(SetupReactiveMode(refSub, cmd), PropagationDirection.DOWN)
    // send reactive command to content actor.
    contentHolder ! TopicContentProtocol.ServeReactive(refSub, cmd)
  }

  def messageSaved(message: Message, replyTo : ActorRef) = {
    sendToSubscribers(message)
    // forward message to parent for ancestor visibility
    propagateToDirection(ForwardToSubscribers(message), PropagationDirection.UP)
    replyTo ! TopicProtocol.MessagePublished
  }

  def sendToSubscribers(message: Message) = {
    subscribers.foreach { actorRef => actorRef ! message }  // push to subscribers
    statisticsHolder ! TopicStatProtocol.MessageReceived  // report stats
  }

  def subscribe(subscriber: ActorRef) = {
    if (!subscribers.contains(subscriber)){
      context.watch(subscriber)
      subscribers += subscriber
      subscriber ! SubscriberProtocol.AcknowledgeSub(self)
      statisticsHolder ! TopicStatProtocol.SubscriberAdded       // report stats
    }
  }

  def unsubscribe(subscriber: ActorRef) = {
    context.unwatch(subscriber)
    subscribers -= subscriber
    subscriber ! SubscriberProtocol.AcknowledgeUnsub(self)
    statisticsHolder ! TopicStatProtocol.SubscriberRemoved    // report stats
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
      statisticsHolder ! TopicStatProtocol.SubTopicAdded     // report stats
    }
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
  case object MessagePublished

  // container used to propagate operation in topic tree
  case class Propagation(message: TopicProtocol.Operation, direction: PropagationDirection)
  trait Operation{}
}

object Topic {  
  def props(topic: String) = Props(classOf[Topic], topic).withDispatcher("topics-dispatcher")
}