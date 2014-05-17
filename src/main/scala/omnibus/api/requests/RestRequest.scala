package omnibus.api.request

import akka.actor._

import spray.routing._

import omnibus.metrics.Instrumented
import omnibus.configuration._
import omnibus.api.exceptions.RestRequestTimeoutException
import omnibus.api.request.RestRequestProtocol._

abstract class RestRequest(ctx : RequestContext) extends Actor with ActorLogging with Instrumented {

  implicit def system = context.system
  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(system).Timeout)

  val timeoutScheduler = system.scheduler.scheduleOnce(timeout.duration, self, RestRequestProtocol.RequestTimeout)
  
  val timerCtx = metrics.timer("timer").timerContext()
  
  override def receive : Receive = handleTimeout

  def handleTimeout : Receive = {
    case RequestTimeout => {
      ctx.complete(new RestRequestTimeoutException())
      metrics.meter("timeout").mark()
      requestOver()
    }  
  }

  def requestOver() = {
    timerCtx.stop()
    self ! PoisonPill
  }

  override def postStop() = {
    timeoutScheduler.cancel()
  }  
}

object RestRequestProtocol {
  case object RequestTimeout
}