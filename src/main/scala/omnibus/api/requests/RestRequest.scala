package omnibus.api.request

import akka.actor._
import akka.actor.SupervisorStrategy.Stop

import scala.util._

import spray.routing._
import spray.http._
import spray.http.StatusCode
import spray.httpx.marshalling._
import spray.json._

import HttpHeaders._
import DefaultJsonProtocol._

import omnibus.api.endpoint.JsonSupport._
import omnibus.metrics.Instrumented
import omnibus.configuration._
import omnibus.api.exceptions.RequestTimeoutException

abstract class RestRequest(ctx: RequestContext) extends Actor with Instrumented {

  implicit def system = context.system
  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(system).Timeout)

  context.setReceiveTimeout(timeout.duration)

  val timerCtx = metrics.timer("request").timerContext()

  def receive = {
    case ReceiveTimeout => requestOver(new RequestTimeoutException())
    case Failure(e)     => requestOver(e)
    case e: Exception   => requestOver(e)
  }

  def closeThings() {
    timerCtx.stop()
    self ! PoisonPill
  }

  def requestOver[T](payload: T)(implicit marshaller: ToResponseMarshaller[T]) = {
    ctx.complete(payload)
    closeThings()
  }

  def requestOver[T](status: StatusCode, payload: T)(implicit marshaller: ToResponseMarshaller[(StatusCode, T)]) = {
    ctx.complete(status, payload)
    closeThings()
  }

  def requestOver[T](status: StatusCode, headers: Seq[HttpHeader], payload: T)(implicit marshaller: ToResponseMarshaller[(StatusCode, Seq[HttpHeader], T)]) = {
    ctx.complete(status, headers, payload)
    closeThings
  }

  override val supervisorStrategy =
    OneForOneStrategy() {
      case e => {
        ctx.complete(e)
        timerCtx.stop()
        Stop
      }
    }
}
