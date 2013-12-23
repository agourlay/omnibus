package omnibus.domain

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps

import omnibus.domain.SubscriberProtocol._
import omnibus.domain.ReactiveMode._

class Subscriber(val responder:ActorRef,  val topics:Set[ActorRef], val mode : ReactiveMode) extends Actor with ActorLogging {

  implicit def executionContext = context.dispatcher
  
  var pendingTopic : Set[ActorRef] = topics
  var topicListened : Set[ActorRef] = Set.empty[ActorRef]

  override def preStart(): Unit = {
    val prettyTopics = prettySubscription
    log.info(s"Creating sub on topics $prettyTopics with mode $mode")

    // subscribe to every topic
    for(topic <- topics){
      topic ! TopicProtocol.Subscribe(self)
    }

    // apply mode
    mode match {
      case ReactiveMode.REPLAY => askReplay()
      case ReactiveMode.LAST   => askLast()
      case ReactiveMode.SIMPLE => log.info("simple subscription")
    }
  }

  def receive = {
  	case AcknowledgeSub(topicRef)       => ackSubscription(topicRef)
  	case AcknowledgeUnsub(topicRef)     => topicListened -= topicRef
    case PushMessage(message : Message) => responder ! message
    case StopSubscription               => self ! PoisonPill
  }

  def askReplay() {
    for(topic <- topics){
      topic ! TopicProtocol.Replay(self)
    }
  }

   def askLast() {
    for(topic <- topics){
      topic ! TopicProtocol.Last(self)
    }
  }

  def ackSubscription(topicRef : ActorRef) = {
    topicListened += topicRef
    pendingTopic -= topicRef
    log.info(s"subscriber $self succesfully subscribed to $topicRef")
  }

  def prettySubscription() : String = {
    val setOfTopic = topics.map(_.path)
                     .map(_.toString)
                     .map(_.split("/topic-repository").toList(1))
    setOfTopic.mkString(" + ")
  }
}

object SubscriberProtocol {
	case class AcknowledgeSub(topic : ActorRef)
	case class AcknowledgeUnsub(topic : ActorRef)
  case class PushMessage(message : Message)
  case object StopSubscription
}