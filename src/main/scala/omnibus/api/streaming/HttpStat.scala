package omnibus.api.streaming

import akka.actor._

import spray.http._

import scala.concurrent.duration._
import scala.language.postfixOps

import omnibus.api.endpoint.JsonSupport._
import omnibus.api.stats._
import omnibus.configuration._
import omnibus.api.streaming.HttpStatProtocol._

class HttpStat(responder: ActorRef, statsRepo : ActorRef) extends StreamingResponse(responder) {

  implicit def executionContext = context.dispatcher

  val statScheduler = context.system.scheduler.schedule(1.second, 1.second, self, HttpStatProtocol.RequestHttpStats)

  override def receive = ({
    case RequestHttpStats => statsRepo ! HttpStatisticsProtocol.LiveStats
    case stat : HttpStats => responder ! MessageChunk("data: "+ formatHttpServerStats.write(stat) +"\n\n")
  }: Receive) orElse super.receive

  override def postStop() = {
    statScheduler.cancel()
  } 
}

object HttpStatProtocol {
  object RequestHttpStats
}

object HttpStat {
  def props(responder: ActorRef, statsRepo : ActorRef) = Props(classOf[HttpStat], responder, statsRepo).withDispatcher("streaming-dispatcher")
}