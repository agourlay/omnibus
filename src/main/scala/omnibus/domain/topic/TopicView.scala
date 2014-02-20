package omnibus.domain.topic

import omnibus.domain._

case class TopicView(topic:String
	               , subTopicsNumber : Long
	               , children : Seq[String]
	               , subscribersNumber : Long
	               , numEvents : Long
	               , creationDate : Long
	               , viewDate: Long = System.currentTimeMillis / 1000)
