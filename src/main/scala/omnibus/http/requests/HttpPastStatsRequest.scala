package omnibus.http.request

import akka.pattern._
import akka.actor._
import akka.actor.Status._

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.httpx.encoding._
import spray.routing._
import spray.routing.authentication._
import spray.json._
import spray.httpx.marshalling._
import spray.http._
import HttpHeaders._
import MediaTypes._

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import DefaultJsonProtocol._
import reflect.ClassTag

import omnibus.http.CustomMediaType
import omnibus.http.JsonSupport._
import omnibus.http.streaming._
import omnibus.configuration._
import omnibus.domain._
import omnibus.domain.subscriber._
import omnibus.repository._
import omnibus.http.stats._
import omnibus.http.stats.HttpStatisticsProtocol._

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
     = Props(classOf[HttpPastStatsRequest], ctx, httpStatService)
}