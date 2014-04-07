package omnibus.domain.topic

import omnibus.domain._
import omnibus.domain.message._

case class TopicStatisticValue(topic: String
	                         , throughputPerSec : Double
	                         , subscribersNumber: Long
	                         , subTopicsNumber : Long
                             , timestamp: Long = System.currentTimeMillis / 1000)

case class TopicRepoStateValue(seqNumber : Long, topicPath :TopicPath)

case class TopicRepoState(events: List[TopicRepoStateValue] = Nil) {
  def update(msg: TopicRepoStateValue) = copy(msg :: events)
  def size = events.length
  override def toString: String = events.reverse.toString
}
