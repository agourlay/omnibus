package omnibus.service.streamed

import omnibus.core.actors.CommonActor

trait StreamedService extends CommonActor {
  val timerCtx = metrics.timer("streamedService").timerContext()

  override def postStop() = {
    timerCtx.stop()
  }
}

trait StreamChunk
case object EndOfStream
case object TimeOutStream
