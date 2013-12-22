package omnibus.domain

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps

import omnibus.domain.SubscriberProtocol._

class Subscriber(val responder:ActorRef,  val topics:Set[ActorRef]) extends Actor with ActorLogging {

  implicit def executionContext = context.dispatcher
  
  var pendingTopic : Set[ActorRef] = topics
  var topicListened : Set[ActorRef] = Set.empty[ActorRef]


  for(topic <- topics){
  	topic ! TopicProtocol.Subscribe(self)
  }


  def receive = {
  	case AcknowledgeSub(topicRef) => {topicListened += topicRef; pendingTopic -= topicRef}
  	case AcknowledgeUnsub(topicRef) => topicListened -= topicRef
    case PushMessage(message : Message) => responder ! message
    case StopSubscription => self ! PoisonPill
  }
}

object SubscriberProtocol {
	case class AcknowledgeSub(topic : ActorRef)
	case class AcknowledgeUnsub(topic : ActorRef)
  case class PushMessage(message : Message)
  case object StopSubscription
}