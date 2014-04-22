package omnibus.api.streaming

import akka.actor._

import spray.http._

import omnibus.api.endpoint.JsonSupport._
import omnibus.domain.topic._

class HttpTopicLeaves(responder: ActorRef, roots: List[ActorRef]) extends StreamingResponse(responder) {
  
  for (root <- roots) root ! TopicProtocol.Leaves(self)

  override def receive = ({
    case topic : TopicView => responder ! MessageChunk("data: "+ formatTopicView.write(topic) +"\n\n")
  }: Receive) orElse super.receive
}

object HttpTopicLeaves {
  def props(responder: ActorRef, roots: List[ActorRef]) = 
      Props(classOf[HttpTopicLeaves], responder, roots).withDispatcher("streaming-dispatcher")
}