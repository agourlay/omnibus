package omnibus.domain.subscriber

import akka.actor._
import omnibus.domain._

case class SubscriberView(ref : ActorRef
	                    , id : String
	                    , topic: String
	                    , ip : String
	                    , creationDate: Long = System.currentTimeMillis / 1000)
