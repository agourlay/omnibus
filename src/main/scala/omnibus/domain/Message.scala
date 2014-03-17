package omnibus.domain

import spray.http._

import omnibus.domain.topic.TopicPath

case class Message(id: Long
	              ,topicPath: TopicPath
	              ,payload: String
	              ,timestamp: Long = System.currentTimeMillis / 1000){
	// secure the application by avoiding huge payload (number completely arbitrary)
	require(payload.size < 100000 , s"Message payload is too big \n")
}

object MessageObj {
  def toMessageChunk(message: Message): MessageChunk = {
    MessageChunk("id: " + message.id + "\n" +
      "event: " + message.topicPath.prettyStr() + "\n" +
      "data: " + message.payload + "\n" +
      "timestamp: " + message.timestamp + "\n\n")
  }
}