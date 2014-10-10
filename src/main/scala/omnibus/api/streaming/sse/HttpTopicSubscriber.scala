package omnibus.api.streaming.sse

import akka.actor.{ Actor, ActorRef, Props }

import omnibus.domain.topic.TopicEvent
import omnibus.domain.subscriber.ReactiveCmd
import omnibus.api.streaming.sse.ServerSentEventSupport._

class HttpTopicSubscriber(responder: ActorRef, cmd: ReactiveCmd) extends ServerSentEventResponse(responder) {

  override def receive = ({
    case te: TopicEvent ⇒ responder ! toChunkFormat(te)
  }: Receive) orElse super.receive
}

object HttpTopicSubscriber {
  def props(responder: ActorRef, cmd: ReactiveCmd) = Props(classOf[HttpTopicSubscriber], responder, cmd).withDispatcher("streaming-dispatcher")
}