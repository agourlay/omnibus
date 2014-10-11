package omnibus.api.streaming.sse

import akka.actor._

import scala.util.Failure

import spray.http._
import spray.routing._
import HttpHeaders._
import spray.can.Http
import spray.httpx.marshalling._
import spray.json._

import omnibus.core.actors.CommonActor
import omnibus.domain.topic.TopicView
import omnibus.api.streaming.StreamingResponse
import omnibus.api.streaming.sse.ServerSentEventSupport.EventStreamType
import omnibus.api.streaming.sse.ServerSentEventSupport._

class ServerSentEventResponse(ctx: RequestContext) extends StreamingResponse[MessageChunk] {

  val responder = ctx.responder

  lazy val responseStart = HttpResponse(
    entity = HttpEntity(EventStreamType, "Omnibus SSE streaming...\n"),
    headers = `Cache-Control`(CacheDirectives.`no-cache`) :: Nil
  )

  override def preStart() = {
    super.preStart()
    responder ! ChunkedResponseStart(responseStart)
  }

  override def postStop() = {
    super.postStop()
    responder ! ChunkedMessageEnd
  }

  override def streamTimeout() {
    responder ! ChunkedMessageEnd
  }

  override def endOfStream() {
    responder ! ChunkedMessageEnd
  }

  override def receive = receiveChunks orElse super.receive

  def receiveChunks: Receive = {
    case topicView: TopicView ⇒ responder ! toChunkFormat(topicView)
    case ev: Http.ConnectionClosed ⇒
      log.debug("Stopping response streaming due to {}", ev)
      closeThings()
    case ReceiveTimeout ⇒ responder ! MessageChunk(":\n") // Comment to keep connection alive
    case Failure(e)     ⇒ requestOver(e)
    case e: Exception   ⇒ requestOver(e)
  }

  def closeThings() {
    timerCtx.stop()
    self ! PoisonPill
  }

  def requestOver[T](payload: T)(implicit marshaller: ToResponseMarshaller[T]) = {
    ctx.complete(payload)
    closeThings()
  }
}

object ServerSentEventResponse {
  def props(ctx: RequestContext) =
    Props(classOf[ServerSentEventResponse], ctx).withDispatcher("streaming-dispatcher")
}