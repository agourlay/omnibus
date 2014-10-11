package omnibus.api.streaming

import akka.actor._

import omnibus.core.actors.CommonActor
import omnibus.service.streamed.{ EndOfStream, TimeoutStream }

trait StreamingResponse[B] extends CommonActor {

  val timerCtx = metrics.timer("streaming").timerContext()

  override def postStop() = {
    timerCtx.stop()
  }

  override def receive = {
    case TimeoutStream ⇒ streamTimeout()
    case EndOfStream   ⇒ endOfStream()
  }

  def toChunkFormat[A, F <: StreamingFormat[A, B]](event: A)(implicit fmt: F) = fmt.format(event)

  def streamTimeout()
  def endOfStream()
}

trait StreamingFormat[A, B] {
  def format(a: A): B
}