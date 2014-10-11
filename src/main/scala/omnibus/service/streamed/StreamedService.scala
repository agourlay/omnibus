package omnibus.service.streamed

import akka.actor._

import omnibus.core.actors.CommonActor

abstract class StreamedService(replyTo: ActorRef) extends CommonActor {
  val timerCtx = metrics.timer("streaming").timerContext()

  override def postStop() = {
    timerCtx.stop()
  }
}

case object EndOfStream
case object TimeOutStream
