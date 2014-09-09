package omnibus.domain.subscriber


object SubscriberSupport extends Enumeration {
  type SubscriberSupport = Value

  val SSE = Value("Server-Sent-Event")
  val WS = Value("Websocket")
}