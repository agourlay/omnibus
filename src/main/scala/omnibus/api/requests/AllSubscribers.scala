package omnibus.api.request

import akka.actor._

import spray.httpx.SprayJsonSupport._
import spray.routing._
import spray.json._

import DefaultJsonProtocol._

import omnibus.api.endpoint.JsonSupport._
import omnibus.domain.subscriber._
import omnibus.domain.subscriber.SubscriberRepositoryProtocol._

class AllSubscribers(ctx : RequestContext, subRepo: ActorRef) extends RestRequest(ctx) {

  subRepo ! SubscriberRepositoryProtocol.AllSubs

  override def receive = waitingLookup orElse handleTimeout

  def waitingLookup : Receive = {
    case Subscribers(subs) => {
      ctx.complete(subs)
      requestOver()
    }  
  }
}

object AllSubscribers {
   def props(ctx : RequestContext, subRepo: ActorRef) 
     = Props(classOf[AllSubscribers], ctx, subRepo).withDispatcher("requests-dispatcher")
}