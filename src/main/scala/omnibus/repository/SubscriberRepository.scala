package omnibus.repository

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps

import omnibus.domain.subscriber.Subscriber
import omnibus.domain.subscriber.ReactiveCmd
import omnibus.repository.SubscriberRepositoryProtocol._

class SubscriberRepository extends Actor with ActorLogging {

  implicit def executionContext = context.dispatcher

  def receive = {
    case CreateSub(topics, responder, reactiveCmd, http) => createSub(topics, responder, reactiveCmd, http)
  }

  def createSub(topics: Set[ActorRef], responder: ActorRef, reactiveCmd: ReactiveCmd, http: Boolean) = {
    log.debug("Creating sub on topics " + topics)
    context.actorOf(Subscriber.props(responder, topics, reactiveCmd, http))
  }
}

object SubscriberRepositoryProtocol {
  case class CreateSub(topics: Set[ActorRef], responder: ActorRef, reactiveCmd: ReactiveCmd, http: Boolean)
}

object SubscriberRepository {
	def props : Props = Props(classOf[SubscriberRepository])
}