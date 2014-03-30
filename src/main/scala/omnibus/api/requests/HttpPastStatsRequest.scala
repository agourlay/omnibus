package omnibus.api.request

import akka.actor._

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.routing._
import spray.http._

import DefaultJsonProtocol._

import omnibus.api.endpoint.JsonSupport._
import omnibus.api.stats._
import omnibus.api.stats.HttpStatisticsProtocol._

class HttpPastStatsRequest(ctx : RequestContext, httpStatService: ActorRef) extends RestRequest(ctx) {

  httpStatService ! PastStats

  override def receive = waitingHttpPastStats orElse handleTimeout

  def waitingHttpPastStats : Receive = {
    case Stats(stats) => {
      ctx.complete (stats)
      self ! PoisonPill
    }
  }
}

object HttpPastStatsRequest {
   def props(ctx : RequestContext, httpStatService: ActorRef) 
     = Props(classOf[HttpPastStatsRequest], ctx, httpStatService).withDispatcher("requests-dispatcher")
}