package omnibus.api.streaming.ws

import spray.can.websocket.frame.TextFrame

import omnibus.domain.topic.{ TopicEvent, TopicView }
import omnibus.api.streaming.StreamingFormat
import omnibus.api.endpoint.JsonSupport._

object WebSocketSupport {

  trait WebSocketFormat[A] extends StreamingFormat[A, TextFrame] {
    def format(a: A): TextFrame
  }

  implicit def topicEventWS: WebSocketFormat[TopicEvent] = new WebSocketFormat[TopicEvent] {
    def format(te: TopicEvent): TextFrame =
      TextFrame("id: " + te.id + "\n" +
        "event: " + te.topicPath.prettyStr() + "\n" +
        "data: " + te.payload + "\n" +
        "timestamp: " + te.timestamp + "\n\n")
  }

  implicit def topicViewWS: WebSocketFormat[TopicView] = new WebSocketFormat[TopicView] {
    def format(tv: TopicView): TextFrame =
      TextFrame("data: " + formatTopicView.write(tv) + "\n\n")
  }
}
