package omnibus.domain

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps

import omnibus.domain.TopicProtocol._

class Topic extends Actor with ActorLogging {

  implicit def executionContext = context.dispatcher

  var messages : Seq[Message] = Seq.empty[Message]
  var subscribers = Map.empty[String, String]

  def receive = {
    case SubscriberNumber => sender ! subscribers.size.toString
  }
}

object TopicProtocol {
  case object Subscribe
  case object Unsubscribe
  case object SubscriberNumber
}