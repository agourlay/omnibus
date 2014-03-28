package omnibus.domain.subscriber

import akka.actor._

case class SubscriberView(ref : ActorRef
	                    , id : String
	                    , topic: String
	                    , ip : String
	                    , mode : String
	                    , creationDate: Long = System.currentTimeMillis / 1000)
