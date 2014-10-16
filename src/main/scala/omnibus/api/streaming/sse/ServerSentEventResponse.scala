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
import omnibus.domain.topic.{ TopicView, TopicEvent }
import omnibus.api.streaming.StreamingResponse
import omnibus.api.streaming.sse.ServerSentEventSupport.EventStreamType
import omnibus.api.streaming.sse.ServerSentEventSupport._

class ServerSentEventResponse(ctx: RequestContext, props: Props) extends StreamingResponse[MessageChunk] {

  val responder = ctx.responder
  val streamingService = context.actorOf(props)

  val responseStart = HttpResponse(
    entity = HttpEntity(EventStreamType, "Omnibus SSE streaming...\n"),
    headers = `Cache-Control`(CacheDirectives.`no-cache`) :: Nil
  )

  override def preStart() = {
    super.preStart()
    responder ! ChunkedResponseStart(responseStart)
  }

  override def postStop() = {
    responder ! ChunkedMessageEnd
    super.postStop()
  }

  override def streamTimeout() {
    responder ! MessageChunk("data: Stream Timeout \n")
    self ! PoisonPill
  }

  override def endOfStream() {
    responder ! MessageChunk("data: End of Stream \n")
    self ! PoisonPill
  }

  override def handleException(e: Throwable) {
    ctx.complete(e)
    self ! PoisonPill
  }

  override def push(mc: MessageChunk) { responder ! mc }

  override def receive = receiveChunks orElse super.receive

  def receiveChunks: Receive = {
    case topicEvent: TopicEvent ⇒ push(toChunkFormat(topicEvent))
    case topicView: TopicView   ⇒ push(toChunkFormat(topicView))
    case ReceiveTimeout         ⇒ push(MessageChunk(":\n")) // Comment to keep connection alive
    case ev: Http.ConnectionClosed ⇒
      log.debug("Stopping response streaming due to {}", ev)
      self ! PoisonPill
  }
}

object ServerSentEventResponse {
  def props(ctx: RequestContext, props: Props) = Props(classOf[ServerSentEventResponse], ctx, props).withDispatcher("streaming-dispatcher")
}