package omnibus.domain.topic

import spray.http.Uri
import omnibus.domain._

case class TopicView(topic:String
	               , subTopicsNumber : Long
	               , children : Seq[String]
	               , viewDate: Long = System.currentTimeMillis / 1000)
