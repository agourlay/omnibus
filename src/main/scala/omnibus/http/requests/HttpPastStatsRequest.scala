package omnibus.http.request

import akka.actor._

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.routing._
import spray.http._

import DefaultJsonProtocol._

import omnibus.http.JsonSupport._
import omnibus.http.stats._

class HttpPastStatsRequest(ctx : RequestContext, httpStatService: ActorRef) extends RestRequest(ctx) {

  httpStatService ! HttpStatisticsProtocol.PastStats

  override def receive = waitingHttpPastStats orElse handleTimeout

  def waitingHttpPastStats : Receive = {
    case stats : List[HttpStats]  => {
      ctx.complete (stats)
      self ! PoisonPill
    }
  }
}

object HttpPastStatsRequest {
   def props(ctx : RequestContext, httpStatService: ActorRef) 
     = Props(classOf[HttpPastStatsRequest], ctx, httpStatService).withDispatcher("requests-dispatcher")
}