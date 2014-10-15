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

  val timerCtx = metrics.timer("service").timerContext()

  override def postStop() = {
    timerCtx.stop()
  }

  def receive = {
    case ReceiveTimeout ⇒ context.parent ! TimeOutService
    case Failure(e)     ⇒ context.parent ! ErrorService(e)
    case e: Exception   ⇒ context.parent ! ErrorService(e)
  }
}

trait ServiceResult
case class ErrorService(e: Throwable)
case object TimeOutService
