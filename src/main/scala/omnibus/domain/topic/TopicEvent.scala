package omnibus.domain.topic

case class TopicEvent(id: Long, topicPath: TopicPath, payload: String, timestamp: Long = System.currentTimeMillis / 1000) {
  // secure the application by avoiding huge payload (number completely arbitrary)
  require(payload.length < 100000, s"TopicEvent payload is too big \n")
}