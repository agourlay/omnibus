package omnibus.api.request

import akka.actor._

import spray.httpx.SprayJsonSupport._
import spray.routing._

import omnibus.api.endpoint.JsonSupport._
import omnibus.api.stats._

class HttpLiveStatsRequest(ctx : RequestContext, httpStatService: ActorRef) extends RestRequest(ctx) {

  httpStatService ! HttpStatisticsProtocol.LiveStats

  override def receive = waitingHttpLiveStats orElse handleTimeout

  def waitingHttpLiveStats : Receive = {
    case stat : HttpStats  => {
      ctx.complete (stat)
      self ! PoisonPill
    }
  }
}

object HttpLiveStatsRequest {
   def props(ctx : RequestContext, httpStatService: ActorRef) 
     = Props(classOf[HttpLiveStatsRequest], ctx, httpStatService).withDispatcher("requests-dispatcher")
}