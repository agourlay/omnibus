package omnibus.service.classic

import akka.actor._

import omnibus.domain.subscriber._
import omnibus.domain.subscriber.SubscriberRepositoryProtocol._

class AllSubscribers(subRepo: ActorRef) extends ClassicService {

  subRepo ! SubscriberRepositoryProtocol.AllSubs

  override def receive = super.receive orElse waitingLookup

  def waitingLookup: Receive = {
    case s @ Subscribers(subs) ⇒ context.parent forward s
  }
}

object AllSubscribers {
  def props(subRepo: ActorRef) = Props(classOf[AllSubscribers], subRepo).withDispatcher("requests-dispatcher")
}