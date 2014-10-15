package omnibus.service.classic

import akka.actor._

import omnibus.domain.subscriber._
import omnibus.domain.subscriber.SubscriberRepositoryProtocol._

class Subscriber(subId: String, subRepo: ActorRef) extends ClassicService {

  subRepo ! SubscriberRepositoryProtocol.SubById(subId)

  override def receive = super.receive orElse waitingLookup

  def waitingLookup: Receive = {
    case sub: SubscriberView ⇒ context.parent forward sub
  }
}

object Subscriber {
  def props(subId: String, subRepo: ActorRef) = Props(classOf[Subscriber], subId, subRepo).withDispatcher("requests-dispatcher")
}