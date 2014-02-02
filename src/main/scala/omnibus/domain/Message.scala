package omnibus.domain

import spray.json._
import spray.http._

case class Message(id: Long
	              ,topicName: String
	              ,payload: String
	              ,timestamp: Long = System.currentTimeMillis / 1000)

object MessageObj {
  def toMessageChunk(message: Message): MessageChunk = {
    MessageChunk("id: " + message.id + "\n" +
      "event: " + message.topicName + "\n" +
      "data: " + message.payload + "\n" +
      "timestamp: " + message.timestamp + "\n\n")
  }
}