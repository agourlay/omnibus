package omnibus.api.streaming.sse

import akka.actor.{ Actor, ActorRef, Props }

import spray.routing._

import omnibus.domain.topic.TopicEvent
import omnibus.domain.subscriber.ReactiveCmd
import omnibus.api.streaming.sse.ServerSentEventSupport._

class HttpTopicSubscriber(ctx: RequestContext, cmd: ReactiveCmd) extends ServerSentEventResponse(ctx) {

  override def receive = ({
    case te: TopicEvent ⇒ ctx.responder ! toChunkFormat(te)
  }: Receive) orElse super.receive
}

object HttpTopicSubscriber {
  def props(ctx: RequestContext, cmd: ReactiveCmd) = Props(classOf[HttpTopicSubscriber], ctx, cmd).withDispatcher("streaming-dispatcher")
}