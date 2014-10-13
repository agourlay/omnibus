package omnibus.service.streamed

import akka.actor._

import omnibus.core.actors.CommonActor

trait StreamedService extends CommonActor {
  val timerCtx = metrics.timer("streaming").timerContext()

  override def postStop() = {
    timerCtx.stop()
  }
}

trait StreamChunk
case object EndOfStream
case object TimeOutStream
