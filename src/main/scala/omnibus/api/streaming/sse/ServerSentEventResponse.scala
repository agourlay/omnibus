package omnibus.api.streaming.sse

import akka.actor._

import spray.http._
import HttpHeaders._
import spray.can.Http

import omnibus.core.actors.CommonActor
import omnibus.api.streaming.StreamingResponse
import omnibus.api.streaming.sse.ServerSentEventSupport.EventStreamType

class ServerSentEventResponse(responder: ActorRef) extends StreamingResponse[MessageChunk] {

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

  def receive = {
    case ev: Http.ConnectionClosed ⇒
      log.debug("Stopping response streaming due to {}", ev)
      self ! PoisonPill
    case ReceiveTimeout ⇒ responder ! MessageChunk(":\n") // Comment to keep connection alive  
  }

}