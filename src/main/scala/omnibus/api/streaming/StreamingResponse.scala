package omnibus.api.streaming

import akka.actor._

import omnibus.core.actors.CommonActor

trait StreamingResponse[B] extends CommonActor {

  val timerCtx = metrics.timer("streaming").timerContext()

  override def postStop() = {
    timerCtx.stop()
  }

  def toChunkFormat[A, F <: StreamingFormat[A, B]](event: A)(implicit fmt: F) = fmt.format(event)
}

trait StreamingFormat[A, B] {
  def format(a: A): B
}