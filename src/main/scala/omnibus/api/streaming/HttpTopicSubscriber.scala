package omnibus.api.streaming

import akka.actor._

import omnibus.domain._
import omnibus.domain.message._
import omnibus.domain.subscriber._

class HttpTopicSubscriber(responder: ActorRef, cmd : ReactiveCmd, topicsPath : String) extends StreamingResponse(responder) {

  override def receive = ({
    case msg: Message => responder ! MessageObj.toMessageChunk(msg) 
  }: Receive) orElse super.receive
}

object HttpTopicSubscriber {
  def props(responder: ActorRef, cmd : ReactiveCmd, topicsPath : String) =
      Props(classOf[HttpTopicSubscriber], responder, cmd, topicsPath).withDispatcher("streaming-dispatcher")
}