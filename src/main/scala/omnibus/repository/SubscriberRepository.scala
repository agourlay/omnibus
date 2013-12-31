package omnibus.repository

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps

import omnibus.http.HttpSubscriber
import omnibus.domain.Subscriber
import omnibus.domain.ReactiveCmd
import omnibus.repository.SubscriberRepositoryProtocol._

class SubscriberRepository extends Actor with ActorLogging {

  implicit def executionContext = context.dispatcher

  def receive = {
    case CreateSub(topics, responder, reactiveCmd, http) => createSub(topics, responder, reactiveCmd, http)
  }

  def createSub(topics: Set[ActorRef], responder: ActorRef, reactiveCmd: ReactiveCmd, http: Boolean) = {
    if (http) {
      log.debug("Creating http sub on topics " + topics)
      context.actorOf(Props(classOf[HttpSubscriber], responder, topics, reactiveCmd))
    } else {
      log.debug("Creating sub on topics " + topics)
      context.actorOf(Props(classOf[Subscriber], responder, topics, reactiveCmd))
    }
  }
}

object SubscriberRepositoryProtocol {
  case class CreateSub(topics: Set[ActorRef], responder: ActorRef, reactiveCmd: ReactiveCmd, http: Boolean)
}