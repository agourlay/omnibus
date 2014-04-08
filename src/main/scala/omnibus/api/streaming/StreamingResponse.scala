package omnibus.api.streaming

import akka.actor._

import spray.http._
import HttpHeaders._
import spray.can.Http

import omnibus.core.Instrumented
import omnibus.api.endpoint.CustomMediaType

class StreamingResponse(responder: ActorRef) extends Actor with ActorLogging with Instrumented{

  def startText = "Generic streaming...\n"

  metrics.meter("start").mark()
  val endMeter = metrics.meter("end")

  lazy val responseStart = HttpResponse(
 		entity  = HttpEntity(CustomMediaType.EventStreamType, startText),
  	headers = `Cache-Control`(CacheDirectives.`no-cache`) :: Nil
  )

  override def preStart() = {
    super.preStart()
    responder ! ChunkedResponseStart(responseStart)
  }

  override def postStop() = {
    responder ! ChunkedMessageEnd
    endMeter.mark()
  }
  
  def receive = {   
    case ev: Http.ConnectionClosed => {
      log.debug("Stopping response streaming due to {}", ev)
      context.stop(self)
    }
    case ReceiveTimeout => responder ! MessageChunk(":\n") // Comment to keep connection alive  
  }
}