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
import scala.collection.mutable.ListBuffer

import omnibus.domain._
import omnibus.domain.subscriber._
import omnibus.domain.subscriber.ReactiveMode._
import omnibus.domain.subscriber.SubscriberProtocol._

class HttpTopicSubscriber(responder: ActorRef, mode : ReactiveMode, topicsPath : String)
                     extends StreamingResponse(responder) {

  override def startText = s"~~> Streaming updates on topics $topicsPath with mode $mode\n\n"

  override def receive = ({
    case message: Message          => responder ! MessageObj.toMessageChunk(message)     
    case ev: Http.ConnectionClosed => {
      log.info("Stopping response streaming due to {}", ev)
      // kill parent!
      context.parent ! SubscriberProtocol.StopSubscription
      // and kill itself just in case :D
      context.stop(self)
    }  
  }: Receive) orElse super.receive
}

object HttpTopicSubscriber {
  def props(responder: ActorRef, mode : ReactiveMode, topicsPath : String) : Props = Props(classOf[HttpTopicSubscriber], responder, mode, topicsPath)
}