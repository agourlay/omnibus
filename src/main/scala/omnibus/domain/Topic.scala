package omnibus.domain

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection.mutable.ListBuffer

import omnibus.domain.TopicProtocol._

class Topic(val topic:String) extends Actor with ActorLogging {

  implicit def executionContext = context.dispatcher
  
  var messages : ListBuffer[Message] = ListBuffer.empty[Message]
  var subscribers : Set[ActorRef] = Set.empty[ActorRef]
  var subTopics : Map[String, ActorRef] = Map.empty[String, ActorRef]

  override def preStart(): Unit = {
    val myPath = self.path
    log.info(s"Creating new root topic $myPath")
  }

  def receive = {
  	case PublishMessage(message) => publishMessage(message)
  	case Subscribe(subscriber)   => subscribe(subscriber)
  	case Unsubscribe(subscriber) => unsubscribe(subscriber)
    case SubscriberNumber        => sender ! subscribers.size.toString
    case CreateSubTopic(topics)  => createSubTopic(topics)
    case Terminated(refSub)      => subscribers -= refSub
    case Replay(refSub)          => replayHistory(refSub)
    case Last(refSub)            => lastMessage(refSub)
  }

  def publishMessage(message : Message) = {
  	messages += message
  	// push to subscribers
    subscribers.foreach{ actorRef =>
    	actorRef ! message
    }
    // forward message to sub-topics
    subTopics.values foreach { subTopic ⇒
      subTopic ! TopicProtocol.PublishMessage(message)
    }	   
  }

  def subscribe(subscriber : ActorRef) = {
    context.watch(subscriber)
    subscribers += subscriber
    subscriber ! SubscriberProtocol.AcknowledgeSub(self)
  }

  def unsubscribe(subscriber : ActorRef) = {
    context.unwatch(subscriber)
  	subscribers -= subscriber
    subscriber ! SubscriberProtocol.AcknowledgeUnsub(self)
  }

  def createSubTopic(topics : List[String]) = topics match{ 
  	case head :: tail => createTopicAndForward(head , tail)
  	case _            => log.debug("No more sub topic to create")
  }

  def createTopicAndForward(subTopic : String, topics : List[String]) = {
    log.info(s"Create sub topic $subTopic and forward $topics")
    if (subTopics.contains(subTopic)) {
      log.info(s"sub topic $subTopic subTopic exist, forward to sub topics")
      subTopics(subTopic) ! TopicProtocol.CreateSubTopic(topics)
    }else {
      val subTopicActor = context.actorOf(Props(classOf[Topic], subTopic), subTopic)
      subTopics += (subTopic -> subTopicActor)
      subTopicActor ! TopicProtocol.CreateSubTopic(topics)
    }
  }

  def replayHistory(refSub : ActorRef) = {
    if (messages.nonEmpty) {
      messages foreach { message =>
        refSub ! message
      }
    }
  }

  def lastMessage(refSub : ActorRef) = {
    if (messages.nonEmpty) {
      refSub ! messages.head  
    }
  }
}

object TopicProtocol {
  case class PublishMessage(message : Message)
  case class Subscribe(subscriber : ActorRef)
  case class Unsubscribe(subscriber : ActorRef)
  case class Replay(subscriber : ActorRef)
  case class Last(subscriber : ActorRef)
  case class CreateSubTopic(topics : List[String])
  case object SubscriberNumber
}