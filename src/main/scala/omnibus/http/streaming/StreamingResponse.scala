package omnibus.http.streaming

import akka.actor._

import spray.routing._
import spray.http._
import spray.http.MediaTypes._
import HttpHeaders._
import spray.can.Http
import scala.language.postfixOps

import omnibus.http.CustomMediaType


class StreamingResponse(responder: ActorRef) extends Actor with ActorLogging {

  def startText = "Generic streaming...\n"

  lazy val responseStart = HttpResponse(
 		entity  = HttpEntity(CustomMediaType.EventStreamType, startText),
  	headers = `Cache-Control`(CacheDirectives.`no-cache`) :: Nil
  )

  override def preStart() = {
    super.preStart
    responder ! ChunkedResponseStart(responseStart)
  }

  override def postStop() = {
    responder ! ChunkedMessageEnd
  }
  
  def receive = {   
    case ev: Http.ConnectionClosed => {
      log.debug("Stopping response streaming due to {}", ev)
      context.stop(self)
    }
    case ReceiveTimeout => responder ! MessageChunk(":\n") // Comment to keep connection alive  
  }
}