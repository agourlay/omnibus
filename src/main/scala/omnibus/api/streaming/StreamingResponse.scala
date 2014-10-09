package omnibus.api.streaming

import akka.actor._

import spray.http._
import HttpHeaders._
import spray.can.Http

import omnibus.metrics.Instrumented
import omnibus.api.endpoint.ServerSentEventSupport._

class StreamingResponse(responder: ActorRef) extends Actor with ActorLogging with Instrumented {

  val timerCtx = metrics.timer("streaming").timerContext()

  lazy val responseStart = HttpResponse(
    entity = HttpEntity(EventStreamType, "Omnibus streaming...\n"),
    headers = `Cache-Control`(CacheDirectives.`no-cache`) :: Nil
  )

  override def preStart() = {
    super.preStart()
    responder ! ChunkedResponseStart(responseStart)
  }

  override def postStop() = {
    responder ! ChunkedMessageEnd
    timerCtx.stop()
  }

  def receive = {
    case ev: Http.ConnectionClosed ⇒ {
      log.debug("Stopping response streaming due to {}", ev)
      self ! PoisonPill
    }
    case ReceiveTimeout ⇒ responder ! MessageChunk(":\n") // Comment to keep connection alive  
  }

  def toMessageChunk[A](message: A)(implicit fmt: ServerSentEventFormat[A]) = MessageChunk(fmt.format(message))

}