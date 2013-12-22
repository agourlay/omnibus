package omnibus.repository

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps
import omnibus.domain._
import omnibus.repository.SubscriberRepositoryProtocol._

class SubscriberRepository extends Actor with ActorLogging {

  implicit def executionContext = context.dispatcher

  def receive = {
    case CreateHttpSub(topics, responder) => createHttpSub(topics, responder)
    case CreateSub(topics, responder)     => createHttpSub(topics, responder)
  }

  def createHttpSub(topics : List[String], responder : ActorRef) = {
  	context.actorOf(Props(classOf[HttpSubscriber], responder, topics))
  }

  def createSub(topics : List[String], responder : ActorRef) = {
  	context.actorOf(Props(classOf[Subscriber], responder, topics))
  }
}

object SubscriberRepositoryProtocol {
   case class CreateHttpSub(topics : List[String], responder : ActorRef)
   case class CreateSub(topics : List[String], responder : ActorRef)
}