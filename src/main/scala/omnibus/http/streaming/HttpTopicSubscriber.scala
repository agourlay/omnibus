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

class HttpTopicSubscriber(responder: ActorRef, cmd : ReactiveCmd, topicsPath : String)
                     extends StreamingResponse(responder) {

  val react = cmd.react
  val sub = cmd.sub
  override def startText = s"~~> Streaming updates on topics $topicsPath with react $react and sub $sub\n\n"

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
  def props(responder: ActorRef, cmd : ReactiveCmd, topicsPath : String) = Props(classOf[HttpTopicSubscriber], responder, cmd, topicsPath)
}