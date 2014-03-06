package omnibus.http.request

import akka.pattern._
import akka.actor._

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
import scala.util._

import DefaultJsonProtocol._
import reflect.ClassTag

import omnibus.http.JsonSupport._
import omnibus.http.streaming._
import omnibus.configuration._
import omnibus.domain._
import omnibus.domain.topic._
import omnibus.domain.subscriber._
import omnibus.repository._
import omnibus.http.request.RestRequestProtocol._

class RestRequest(ctx : RequestContext) extends Actor with ActorLogging {

  implicit def system = context.system
  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(system).Timeout.Ask)

  system.scheduler.scheduleOnce(timeout.duration, self, RestRequestProtocol.RequestTimeout)
  
  def receive = handleTimeout

  def handleTimeout : Receive = {
    case RequestTimeout => {
      ctx.complete(new AskTimeoutException("request timeout"))
      self ! PoisonPill
    }  
  }
}

object RestRequestProtocol {
  case object RequestTimeout
}