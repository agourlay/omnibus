package omnibus.domain.topic

case class TopicView(topic:String
	               , subTopicsNumber : Long
	               , children : Seq[String]
	               , subscribersNumber : Long
	               , numEvents : Long
	               , throughputPerSec : Double
	               , creationDate : Long
	               , timestamp : Long = System.currentTimeMillis / 1000)
