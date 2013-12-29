package omnibus.domain

import spray.json._
import spray.http._

case class Message(id: Long,
  topicName: String,
  payload: String,
  timestamp: Long = System.currentTimeMillis / 1000)

case class TopicState(events: List[Message] = Nil) {
  def update(msg: Message) = copy(msg :: events)
  def size = events.length
  override def toString: String = events.reverse.toString
}

object MessageObj {
  def toMessageChunk(message: Message): MessageChunk = {
    MessageChunk("id: " + message.id + "\n" +
      "event: " + message.topicName + "\n" +
      "data: " + message.payload + "\n" +
      "timestamp: " + message.timestamp + "\n\n")
  }
}

object PropagationDirection extends Enumeration {
  type PropagationDirection = Value

  val UP = Value("up")
  val DOWN = Value("down")
}