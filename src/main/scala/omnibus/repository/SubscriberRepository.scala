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

  def createHttpSub(topics : Set[ActorRef], responder : ActorRef) = {
    log.info("Creating http sub on topics " + topics )
  	context.actorOf(Props(classOf[HttpSubscriber], responder, topics))
  }

  def createSub(topics : Set[ActorRef], responder : ActorRef) = {
    log.info("Creating sub on topics " + topics )
  	context.actorOf(Props(classOf[Subscriber], responder, topics))
  }
}

object SubscriberRepositoryProtocol {
   case class CreateHttpSub(topics : Set[ActorRef], responder : ActorRef)
   case class CreateSub(topics : Set[ActorRef], responder : ActorRef)
}