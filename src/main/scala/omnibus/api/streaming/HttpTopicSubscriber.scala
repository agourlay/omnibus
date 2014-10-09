package omnibus.api.streaming

import akka.actor.{ Actor, ActorRef, Props }

import omnibus.domain._
import omnibus.domain.message._
import omnibus.domain.subscriber._

class HttpTopicSubscriber(responder: ActorRef, cmd: ReactiveCmd) extends StreamingResponse(responder) {

  override def receive = ({
    case msg: Message ⇒ responder ! toMessageChunk(msg)
  }: Receive) orElse super.receive
}

object HttpTopicSubscriber {
  def props(responder: ActorRef, cmd: ReactiveCmd) = Props(classOf[HttpTopicSubscriber], responder, cmd).withDispatcher("streaming-dispatcher")
}