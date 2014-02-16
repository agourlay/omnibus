package omnibus.domain

import spray.json._
import spray.http._

import omnibus.domain.topic.TopicPath

case class Message(id: Long
	              ,topicPath: TopicPath
	              ,payload: String
	              ,timestamp: Long = System.currentTimeMillis / 1000)

object MessageObj {
  def toMessageChunk(message: Message): MessageChunk = {
    MessageChunk("id: " + message.id + "\n" +
      "event: " + message.topicPath.prettyStr() + "\n" +
      "data: " + message.payload + "\n" +
      "timestamp: " + message.timestamp + "\n\n")
  }
}