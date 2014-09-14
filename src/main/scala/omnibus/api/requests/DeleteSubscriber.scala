package omnibus.api.request

import akka.actor._

import spray.routing._
import spray.http._

import omnibus.domain.subscriber.SubscriberRepositoryProtocol._
import omnibus.domain.subscriber._

class DeleteSubscriber(subId: String, ctx: RequestContext, subRepo: ActorRef) extends RestRequest(ctx) {

  subRepo ! SubscriberRepositoryProtocol.SubById(subId)

  override def receive = super.receive orElse waitingLookup

  def waitingLookup: Receive = {
    case SubLookup(optView) => handleLookup(optView)
  }

  def handleLookup(optView: Option[SubscriberView]) = {
    optView match {
      case None => ctx.complete(new SubscriberNotFoundException(subId))
      case Some(subView) => {
        subRepo ! SubscriberRepositoryProtocol.KillSub(subId)
        context.become(super.receive orElse waitingAck)
      }
    }
  }

  def waitingAck: Receive = {
    case SubKilled(_) => requestOver(StatusCodes.Accepted, s"Subscriber $subId deleted\n")
  }
}

object DeleteSubscriber {
  def props(subId: String, ctx: RequestContext, subRepo: ActorRef) = Props(classOf[DeleteSubscriber], subId, ctx, subRepo).withDispatcher("requests-dispatcher")
}