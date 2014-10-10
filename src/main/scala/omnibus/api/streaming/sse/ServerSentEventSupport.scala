package omnibus.api.streaming.sse

import spray.http._
import spray.http.MediaTypes._
import spray.routing._
import Directives._

import omnibus.domain.topic.{ TopicEvent, TopicView }
import omnibus.api.streaming.StreamingFormat
import omnibus.api.endpoint.JsonSupport._

object ServerSentEventSupport {
  val EventStreamType = register(
    MediaType.custom(
      mainType = "text",
      subType = "event-stream",
      compressible = true,
      binary = false
    )
  )

  def lastEventId = optionalHeaderValueByName("Last-Event-ID") | parameter("lastEventId"?)

  trait ServerSentEventFormat[A] extends StreamingFormat[A, MessageChunk] {
    def format(a: A): MessageChunk
  }

  implicit def topicEventSSE: ServerSentEventFormat[TopicEvent] = new ServerSentEventFormat[TopicEvent] {
    def format(te: TopicEvent): MessageChunk =
      MessageChunk("id: " + te.id + "\n" +
        "event: " + te.topicPath.prettyStr() + "\n" +
        "data: " + te.payload + "\n" +
        "timestamp: " + te.timestamp + "\n\n")
  }

  implicit def topicViewSSE: ServerSentEventFormat[TopicView] = new ServerSentEventFormat[TopicView] {
    def format(tv: TopicView): MessageChunk =
      MessageChunk("data: " + formatTopicView.write(tv) + "\n\n")
  }
}
