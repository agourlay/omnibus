package omnibus.http.request

import akka.actor._
import akka.actor.Status._

import spray.routing._
import spray.http._

import omnibus.repository._

class DeleteSubscriberRequest(subId : String, ctx : RequestContext, subRepo: ActorRef) extends RestRequest(ctx) {

  subRepo ! SubscriberRepositoryProtocol.KillSub(subId)

  override def receive = waitingAck orElse handleTimeout

  def waitingAck : Receive = {
    case true        => {
     ctx.complete(StatusCodes.Accepted, s"Subscriber $subId deleted\n")
      self ! PoisonPill
    }  
    case Failure(ex) => {
      ctx.complete(ex)
      self ! PoisonPill
    }  
  }
}

object DeleteSubscriberRequest {
   def props(subId : String, ctx : RequestContext, subRepo: ActorRef) 
     = Props(classOf[DeleteSubscriberRequest],subId, ctx, subRepo).withDispatcher("requests-dispatcher")
}