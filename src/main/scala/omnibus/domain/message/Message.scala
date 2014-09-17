package omnibus.domain.message

import spray.http._
import spray.can.websocket.frame.{ BinaryFrame, TextFrame }

import omnibus.domain.topic.TopicPath

case class Message(id: Long, topicPath: TopicPath, payload: String, timestamp: Long = System.currentTimeMillis / 1000) {
  // secure the application by avoiding huge payload (number completely arbitrary)
  require(payload.size < 100000, s"Message payload is too big \n")
}

object MessageObj {

  def toSSE(message: Message) = {
    "id: " + message.id + "\n" +
      "event: " + message.topicPath.prettyStr() + "\n" +
      "data: " + message.payload + "\n" +
      "timestamp: " + message.timestamp + "\n\n"
  }

  def toMessageChunk(message: Message) = MessageChunk(toSSE(message))

  def toMessageFrame(message: Message) = TextFrame(toSSE(message))

}