package omnibus.api.request

import akka.actor._

import spray.httpx.SprayJsonSupport._
import spray.routing._

import omnibus.api.endpoint.JsonSupport._
import omnibus.domain.subscriber._
import omnibus.domain.subscriber.SubscriberRepositoryProtocol._

class Subscriber(subId : String, ctx : RequestContext, subRepo: ActorRef) extends RestRequest(ctx) {

  subRepo ! SubscriberRepositoryProtocol.SubById(subId)

  override def receive = super.receive orElse waitingLookup

  def waitingLookup : Receive = {
    case sub : SubscriberView => requestOver(sub)
  }
}

object Subscriber {
   def props(subId : String, ctx : RequestContext, subRepo: ActorRef) 
     = Props(classOf[Subscriber],subId, ctx, subRepo).withDispatcher("requests-dispatcher")
}