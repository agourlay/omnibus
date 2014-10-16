package omnibus.api.streaming

import akka.actor._
import akka.actor.SupervisorStrategy.Stop

import scala.util.Failure

import omnibus.core.actors.CommonActor
import omnibus.service.streamed.{ EndOfStream, TimeOutStream }

trait StreamingResponse[B] extends CommonActor {

  val timerCtx = metrics.timer("streaming").timerContext()

  override def postStop() = {
    timerCtx.stop()
  }

  override def receive = {
    case TimeOutStream ⇒ streamTimeout()
    case EndOfStream   ⇒ endOfStream()
    case Failure(e)    ⇒ handleException(e)
    case e: Exception  ⇒ handleException(e)
    // TODO add generic event handling 
    // case s: StreamChunk =>  push(toChunkFormat(s))
  }

  def push(b: B)

  def handleException(e: Throwable)

  def toChunkFormat[A, F <: StreamingFormat[A, B]](event: A)(implicit fmt: F): B = fmt.format(event)

  def streamTimeout()

  def endOfStream()

  override val supervisorStrategy =
    OneForOneStrategy() {
      case e ⇒ {
        handleException(e)
        Stop
      }
    }
}

trait StreamingFormat[A, B] {
  def format(a: A): B
}