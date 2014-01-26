package omnibus.domain.topic

import omnibus.domain._

case class TopicStatisticState(topic:String
	                         , throughputPerSec : Double
	                         , subscribersNumber: Long
	                         , subTopicsNumber : Long,
                               timestamp: Long = System.currentTimeMillis / 1000)

case class TopicState(events: List[Message] = Nil) {
  def update(msg: Message) = copy(msg :: events)
  def size = events.length
  override def toString: String = events.reverse.toString
}
