package omnibus.repository

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps
import omnibus.domain._
import omnibus.domain.ReactiveMode._
import omnibus.repository.SubscriberRepositoryProtocol._

class SubscriberRepository extends Actor with ActorLogging {

  implicit def executionContext = context.dispatcher

  def receive = {
    case CreateHttpSub(topics, responder, mode) => createHttpSub(topics, responder, mode)
    case CreateSub(topics, responder, mode)     => createSub(topics, responder, mode)
  }

  def createHttpSub(topics : Set[ActorRef], responder : ActorRef, mode : ReactiveMode) = {
    log.info("Creating http sub on topics " + topics )
  	context.actorOf(Props(classOf[HttpSubscriber], responder, topics, mode))
  }

  def createSub(topics : Set[ActorRef], responder : ActorRef, mode : ReactiveMode) = {
    log.info("Creating sub on topics " + topics )
  	context.actorOf(Props(classOf[Subscriber], responder, topics, mode))
  }
}

object SubscriberRepositoryProtocol {
   case class CreateHttpSub(topics : Set[ActorRef], responder : ActorRef, mode : ReactiveMode)
   case class CreateSub(topics : Set[ActorRef], responder : ActorRef, mode : ReactiveMode)
}