package omnibus.domain.topic

case class TopicStatisticState(topicName: String, throughputPerSec : Long, subscribersNumber: Long, subTopicsNumber : Long,
                               timestamp: Long = System.currentTimeMillis / 1000)
