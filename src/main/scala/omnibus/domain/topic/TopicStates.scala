package omnibus.domain.topic

import omnibus.domain._

case class TopicStatisticValue(seqNumber : Long
					               		 , topic: String
	                           , throughputPerSec : Double
	                           , subscribersNumber: Long
	                           , subTopicsNumber : Long
                             , timestamp: Long = System.currentTimeMillis / 1000)

case class TopicStatisticState(events: List[TopicStatisticValue] = Nil) {
  def update(msg: TopicStatisticValue) = copy(msg :: events)
  def size = events.length
  override def toString: String = events.reverse.toString
}

case class MessageTopic(seqNumber : Long, msg : Message)

case class TopicState(events: List[MessageTopic] = Nil) {
  def update(msg: MessageTopic) = copy(msg :: events)
  def size = events.length
  override def toString: String = events.reverse.toString
}

case class TopicRepoStateValue(seqNumber : Long, topicPath :TopicPath)

case class TopicRepoState(events: List[TopicRepoStateValue] = Nil) {
  def update(msg: TopicRepoStateValue) = copy(msg :: events)
  def size = events.length
  override def toString: String = events.reverse.toString
}
