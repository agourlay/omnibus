package omnibus.domain

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

import omnibus.domain.JsonSupport._
import omnibus.domain.ReactiveMode._


class HttpSubscriber(responder:ActorRef, topics:Set[ActorRef], mode : ReactiveMode) extends Subscriber(responder, topics, mode) {
  
  lazy val EventStreamType = register(
                          MediaType.custom(
                            mainType = "text",
                            subType = "event-stream",
                            compressible = false,
                            binary = false
                          ))

  lazy val responseStart = HttpResponse(
      entity  = HttpEntity(EventStreamType, startText),
      headers = `Cache-Control`(CacheDirectives.`no-cache`) :: Nil
      )

  val topicsPath = prettySubscription()
  val startText = s"... Streaming subscription for topics $topicsPath\n"

  override def preStart(): Unit = {
    responder ! ChunkedResponseStart(responseStart) 
    super.preStart
  }

  override def receive = {
    case m : Message => pushMessageSSE(m)     
    case ev: Http.ConnectionClosed => {
      log.debug("Stopping response streaming due to {}", ev)
      context.stop(self)
    }
    case ReceiveTimeout => responder ! MessageChunk(":\n") // Comment to keep connection alive  
    case _              => super.receive   
  }

  def pushMessageSSE(message : Message) = {
    val nextChunk = MessageChunk("data: "+ formatMessage.write(message) +"\n\n")
    responder ! nextChunk 
  }

}
