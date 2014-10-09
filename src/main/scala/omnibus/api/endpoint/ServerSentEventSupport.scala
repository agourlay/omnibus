package omnibus.api.endpoint

import spray.http._
import spray.http.MediaTypes._
import spray.routing._
import Directives._

import omnibus.domain.message.Message

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

  trait ServerSentEventFormat[A] {
    def format(a: A): String
  }

  implicit def messageSSE: ServerSentEventFormat[Message] = new ServerSentEventFormat[Message] {
    def format(message: Message): String = {
      "id: " + message.id + "\n" +
        "event: " + message.topicPath.prettyStr() + "\n" +
        "data: " + message.payload + "\n" +
        "timestamp: " + message.timestamp + "\n\n"
    }
  }
}
