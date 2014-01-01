package omnibus.http.streaming

import akka.actor._
import akka.pattern._

import spray.routing._
import spray.http._
import spray.http.MediaTypes._
import HttpHeaders._
import spray.can.Http
import spray.can.server.Stats

import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.Future

import omnibus.http.JsonSupport._


class HttpStatStream(responder: ActorRef) extends StreamingResponse(responder) {

  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(3 seconds)	
  
  override def startText = s"~~> Streaming http statistics\n"

  override def preStart() = {
    super.preStart
    context.system.scheduler.schedule(1.seconds,1.seconds){
      val stats = (context.actorSelection("/user/IO-HTTP/listener-0") ? Http.GetStats).mapTo[Stats]
      stats pipeTo self
    }
  }

  override def receive = ({
    case stat : Stats => {
        val nextChunk = MessageChunk("data: "+ formatHttpServerStats.write(stat) +"\n\n")
        responder ! nextChunk 
    }
  }: Receive) orElse super.receive
}