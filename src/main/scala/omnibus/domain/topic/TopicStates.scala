package omnibus.domain.topic

case class TopicRepoStateValue(seqNumber: Long, topicPath: TopicPath)

case class TopicRepoState(events: List[TopicRepoStateValue] = Nil) {
  def update(msg: TopicRepoStateValue) = copy(msg :: events)
  def size = events.length
  override def toString: String = events.reverse.toString
}
