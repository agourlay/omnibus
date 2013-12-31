package omnibus.domain

case class TopicStatistiqueState(topicName: String, throughputPerSec : Long, subscribersNumber: Long, subTopicsNumber : Long,
                                 timestamp: Long = System.currentTimeMillis / 1000)
