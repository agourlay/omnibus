package omnibus.api.streaming.sse

import akka.actor.{ Actor, ActorRef, Props }

import spray.http._

import omnibus.domain.topic._
import omnibus.api.streaming.sse.ServerSentEventSupport._

class HttpTopicLeaves(responder: ActorRef, roots: List[ActorRef]) extends ServerSentEventResponse(responder) {

  for (root ← roots) root ! TopicProtocol.Leaves(self)

  override def receive = ({
    case topicView: TopicView ⇒ responder ! toChunkFormat(topicView)
  }: Receive) orElse super.receive
}

object HttpTopicLeaves {
  def props(responder: ActorRef, roots: List[ActorRef]) =
    Props(classOf[HttpTopicLeaves], responder, roots).withDispatcher("streaming-dispatcher")
}