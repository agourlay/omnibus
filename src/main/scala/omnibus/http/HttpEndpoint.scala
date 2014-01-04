package omnibus.http

import akka.pattern._
import akka.actor._

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.httpx.encoding._
import spray.routing._
import spray.can.Http
import spray.can.server.Stats

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util._

import DefaultJsonProtocol._
import reflect.ClassTag

import omnibus.http.JsonSupport._
import omnibus.http.route._
import omnibus.domain._
import omnibus.configuration._
import omnibus.service._
import omnibus.service.OmnibusServiceProtocol._

class HttpEndpoint(omnibusService: ActorRef) extends HttpServiceActor with ActorLogging {
  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(context.system).Timeout.Ask)

  val routes =
    new TopicRoute(omnibusService).route ~ // '/topics'
    new StatsRoute(omnibusService).route ~ // '/stats'
    new AdminRoute(omnibusService).route ~ // '/admin/topics'
    new AdminUIRoute().route               // '/ '

  def receive = runRoute(routes)

}