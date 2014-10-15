package omnibus.service.classic

import akka.actor._

import omnibus.domain.subscriber.SubscriberRepositoryProtocol._
import omnibus.domain.subscriber._

class DeleteSubscriber(subId: String, subRepo: ActorRef) extends ClassicService {

  subRepo ! SubscriberRepositoryProtocol.SubById(subId)

  override def receive = super.receive orElse waitingLookup

  def waitingLookup: Receive = {
    case SubLookup(optView) ⇒
      optView match {
        case None ⇒ context.parent ! new SubscriberNotFoundException(subId)
        case Some(subView) ⇒
          subRepo ! SubscriberRepositoryProtocol.KillSub(subId)
          context.become(super.receive orElse waitingAck)
      }
  }

  def waitingAck: Receive = {
    case s @ SubKilled(id) ⇒ context.parent forward s
  }
}

object DeleteSubscriber {
  def props(subId: String, subRepo: ActorRef) = Props(classOf[DeleteSubscriber], subId, subRepo).withDispatcher("requests-dispatcher")
}