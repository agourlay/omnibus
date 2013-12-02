package omnibus.domain

import spray.json._

case class Message(timestamp : Long, topicName : String, payload :JsValue) 