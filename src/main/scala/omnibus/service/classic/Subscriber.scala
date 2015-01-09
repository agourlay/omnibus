package omnibus.service.classic

import akka.actor.{ ActorRef, Props }

import omnibus.domain.subscriber._

class Subscriber(subId: String, subRepo: ActorRef) extends ClassicService {

  subRepo ! SubscriberRepositoryProtocol.SubById(subId)

  override def receive = super.receive orElse waitingLookup

  def waitingLookup: Receive = {
    case sub: SubscriberView ⇒ returnResult(sub)
  }
}

object Subscriber {
  def props(subId: String, subRepo: ActorRef) = Props(classOf[Subscriber], subId, subRepo)
}