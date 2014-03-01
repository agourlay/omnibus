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
import omnibus.configuration._
import omnibus.domain.topic._


class HttpTopicViewStream(responder: ActorRef, roots: List[ActorRef]) extends StreamingResponse(responder) {

  implicit def executionContext = context.dispatcher
  implicit def system = context.system
  
  override def startText = s"~~> Streaming topic view\n"

  override def preStart() = {
    super.preStart
    for (root <- roots) root ! TopicProtocol.Leaves(self)
  }

  override def receive = ({
    case topic : TopicView => responder ! MessageChunk("data: "+ formatTopicView.write(topic) +"\n\n")
  }: Receive) orElse super.receive
}

object HttpTopicViewStream {
  def props(responder: ActorRef, roots: List[ActorRef]) = 
      Props(classOf[HttpTopicViewStream], responder, roots).withDispatcher("streaming-dispatcher")
}