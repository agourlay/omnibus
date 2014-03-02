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
import omnibus.http.stats._
import omnibus.configuration._
import omnibus.http.streaming.HttpStatStreamProtocol._


class HttpStatStream(responder: ActorRef, statsRepo : ActorRef) extends StreamingResponse(responder) {

  implicit def executionContext = context.dispatcher
  implicit def system = context.system

  implicit val timeout = akka.util.Timeout(Settings(system).Timeout.Ask)
  val sampling = Settings(system).Statistics.Sampling
  
  override def startText = s"~~> Streaming http statistics\n"

  override def preStart() = {
    super.preStart
    context.system.scheduler.schedule(sampling, sampling, self, HttpStatStreamProtocol.RequestHttpStats)
  }

  override def receive = ({
    case RequestHttpStats => statsRepo ! HttpStatisticsProtocol.LiveStats
    case stat : HttpStats => responder ! MessageChunk("data: "+ formatHttpServerStats.write(stat) +"\n\n")
  }: Receive) orElse super.receive
}

object HttpStatStreamProtocol {
  object RequestHttpStats
}

object HttpStatStream {
  def props(responder: ActorRef, statsRepo : ActorRef) = Props(classOf[HttpStatStream], responder, statsRepo).withDispatcher("streaming-dispatcher")
}