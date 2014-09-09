package omnibus.domain.subscriber

import akka.actor.ActorRef

case class SubscriberView(ref : ActorRef
	                    , id : String
	                    , topic: String
	                    , ip : String
	                    , mode : String
	                    , support : String
	                    , creationDate: Long = System.currentTimeMillis / 1000)
