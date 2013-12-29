package omnibus.http

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
import scala.collection.mutable.ListBuffer

import omnibus.domain._
import omnibus.domain.ReactiveMode._
import omnibus.domain.SubscriberProtocol._

class HttpSubscriber(responder: ActorRef, topics: Set[ActorRef], reactiveCmd: ReactiveCmd)
    extends Subscriber(responder, topics, reactiveCmd) {

  lazy val EventStreamType = register(
    MediaType.custom(
      mainType = "text",
      subType = "event-stream",
      compressible = false,
      binary = false
    )
  )

  lazy val responseStart = HttpResponse(
    entity = HttpEntity(EventStreamType, startText),
    headers = `Cache-Control`(CacheDirectives.`no-cache`) :: Nil
  )

  val topicsPath = prettySubscription(topics)
  val mode = reactiveCmd.mode
  val startText = s"~~> Streaming updates on topics $topicsPath with mode $mode\n\n"

  override def preStart() = {
    super.preStart
    responder ! ChunkedResponseStart(responseStart)
  }

  override def receive = ({
    case timeout: ReceiveTimeout   => responder ! MessageChunk(":\n") // Comment to keep connection alive      
    case ev: Http.ConnectionClosed => log.info("Stopping response streaming due to {}", ev); context.stop(self)
  }: Receive) orElse super.receive

  override def formatMessagePayload(message: Message) = {
    MessageObj.toMessageChunk(message)
  }
}
