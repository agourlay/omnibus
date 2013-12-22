package omnibus.domain

import spray.json._

case class Message(topicName : String, payload : String, timestamp : Long = System.currentTimeMillis / 1000)