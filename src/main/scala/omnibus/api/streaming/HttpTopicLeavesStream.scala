package omnibus.api.streaming

import akka.actor._

import spray.http._

import omnibus.api.endpoint.JsonSupport._
import omnibus.domain.topic._

class HttpTopicLeavesStream(responder: ActorRef, roots: List[ActorRef]) extends StreamingResponse(responder) {
  
  override def startText = s"~~> Streaming topic view\n"

  override def preStart() = {
    super.preStart()
    for (root <- roots) root ! TopicProtocol.Leaves(self)
  }

  override def receive = ({
    case topic : TopicView => responder ! MessageChunk("data: "+ formatTopicView.write(topic) +"\n\n")
  }: Receive) orElse super.receive
}

object HttpTopicLeavesStream {
  def props(responder: ActorRef, roots: List[ActorRef]) = 
      Props(classOf[HttpTopicLeavesStream], responder, roots).withDispatcher("streaming-dispatcher")
}