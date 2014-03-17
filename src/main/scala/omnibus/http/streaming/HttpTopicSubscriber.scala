package omnibus.http.streaming

import akka.actor._

import omnibus.domain._
import omnibus.domain.subscriber._


class HttpTopicSubscriber(responder: ActorRef, cmd : ReactiveCmd, topicsPath : String) extends StreamingResponse(responder) {

  val react = cmd.react
  override def startText = s"~~> Streaming updates on topics $topicsPath with react $react\n\n"

  override def receive = ({
    case message: Message => responder ! MessageObj.toMessageChunk(message) 
  }: Receive) orElse super.receive
}

object HttpTopicSubscriber {
  def props(responder: ActorRef, cmd : ReactiveCmd, topicsPath : String) =
      Props(classOf[HttpTopicSubscriber], responder, cmd, topicsPath).withDispatcher("streaming-dispatcher")
}