package omnibus.service.classic

import akka.actor._

import scala.util.Failure

import omnibus.configuration._
import omnibus.core.actors.CommonActor

trait ClassicService extends CommonActor {

  implicit def system = context.system
  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(system).Timeout)

  context.setReceiveTimeout(timeout.duration)

  val timerCtx = metrics.timer("serviceCall").timerContext()
  val exceptionMeter = metrics.meter("exception")

  override def postStop() = {
    timerCtx.stop()
  }

  def returnResult(result: Any) {
    context.parent ! result
    timerCtx.stop()
    self ! PoisonPill
  }

  def returnError(t: Throwable) {
    context.parent ! ServiceError(t)
    timerCtx.stop()
    exceptionMeter.mark()
    self ! PoisonPill
  }

  def receive = {
    case ReceiveTimeout ⇒ context.parent ! ServiceError(new ServiceTimeoutException())
    case Failure(e)     ⇒ context.parent ! ServiceError(e)
    case e: Exception   ⇒ context.parent ! ServiceError(e)
  }
}

class ServiceTimeoutException extends Exception("The service is taking longer than expected")

trait ServiceResult
case class ServiceError(e: Throwable)