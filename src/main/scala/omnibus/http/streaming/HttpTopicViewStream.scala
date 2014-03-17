package omnibus.http.streaming

import akka.actor._

import spray.http._

import omnibus.http.JsonSupport._
import omnibus.domain.topic._


class HttpTopicViewStream(responder: ActorRef, roots: List[ActorRef]) extends StreamingResponse(responder) {

  implicit def executionContext = context.dispatcher
  implicit def system = context.system
  
  override def startText = s"~~> Streaming topic view\n"

  override def preStart() = {
    super.preStart()
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