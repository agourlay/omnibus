package omnibus.service.streamed

import akka.actor._

import scala.util.Failure

import omnibus.configuration._
import omnibus.core.actors.CommonActor

abstract class ClassicService(replyTo: ActorRef) extends CommonActor {

  implicit def system = context.system
  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(system).Timeout)

  context.setReceiveTimeout(timeout.duration)

  val timerCtx = metrics.timer("classic").timerContext()

  override def postStop() = {
    timerCtx.stop()
  }

  def receive = {
    case ReceiveTimeout ⇒ replyTo ! TimeOutService
    case Failure(e)     ⇒ replyTo ! ErrorService(e)
    case e: Exception   ⇒ replyTo ! ErrorService(e)
  }
}

trait ServiceResult
case class ErrorService(e: Throwable)
case object TimeOutService
