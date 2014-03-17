package omnibus.http.request

import akka.actor._

import spray.httpx.SprayJsonSupport._
import spray.routing._

import omnibus.http.JsonSupport._
import omnibus.domain.subscriber._
import omnibus.repository._

class SubscriberRequest(subId : String, ctx : RequestContext, subRepo: ActorRef) extends RestRequest(ctx) {

  subRepo ! SubscriberRepositoryProtocol.SubById(subId)

  override def receive = waitingLookup orElse handleTimeout

  def waitingLookup : Receive = {
    case sub : SubscriberView => {
      ctx.complete(sub)
      self ! PoisonPill
    }  
  }
}

object SubscriberRequest {
   def props(subId : String, ctx : RequestContext, subRepo: ActorRef) 
     = Props(classOf[SubscriberRequest],subId, ctx, subRepo).withDispatcher("requests-dispatcher")
}