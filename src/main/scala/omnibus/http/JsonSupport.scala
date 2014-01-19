package omnibus.http

import java.util.Date
import java.text.SimpleDateFormat
import java.text.ParseException
import java.util.concurrent.TimeUnit

import scala.concurrent.duration._

import spray.json._
import spray.http.Uri
import spray.can.server.Stats
import DefaultJsonProtocol._

import omnibus.domain._
import omnibus.domain.topic._

object JsonSupport {
  implicit val formatMessage = jsonFormat4(Message)
  implicit val formatTopicStats = jsonFormat5(TopicStatisticState)

  implicit val formatTopicView = new RootJsonFormat[TopicView] {
    def write(obj: TopicView): JsValue = JsObject(
      "topic"              -> JsArray(obj.topic.split("/").tail.map(JsString(_)).toList),
      "subTopicsNumber"    -> JsNumber(obj.subTopicsNumber),
      "subscribersNumber"  -> JsNumber(obj.subscribersNumber),
      "eventsNumber"       -> JsNumber(obj.numEvents),
      "viewDate"           -> JsNumber(obj.viewDate),  
      "_embedded"          -> JsObject("children" -> JsArray(
        obj.children.map( child => JsObject( child.split("/").last ->  JsObject("href" -> JsString("/topics"+child)))).toList
        )
      ),
      "_links"             -> JsArray(
        JsObject("self"      -> JsObject("href" -> JsString("/topics"+obj.topic))),
        JsObject("subscribe" -> JsObject("href" -> JsString("/stream/topics"+obj.topic))),
        JsObject("stats"     -> JsObject("href" -> JsString("/stats/topics"+obj.topic)))
      )
    )

    // we don't need to deserialize the view
    def read(json: JsValue): TopicView = ???
  }  

  implicit val formatHttpServerStats = new RootJsonFormat[Stats] {
    def write(obj: Stats): JsValue = JsObject(
      "uptimeInMilli"      -> JsNumber(obj.uptime.toMillis),
      "totalRequests"      -> JsNumber(obj.totalRequests),
      "openRequests"       -> JsNumber(obj.openRequests),
      "maxOpenRequests"    -> JsNumber(obj.maxOpenRequests),
      "totalConnections"   -> JsNumber(obj.totalConnections),
      "openConnections"    -> JsNumber(obj.openConnections),
      "maxOpenConnections" -> JsNumber(obj.maxOpenConnections),
      "requestTimeouts"    -> JsNumber(obj.requestTimeouts)
    )

    def read(json: JsValue): Stats = {
      val fields = json.asJsObject.fields
      val uptimeFields = fields.get("uptime").get.asJsObject.fields
      Stats(
        FiniteDuration(
          uptimeFields.get("length").get.asInstanceOf[JsNumber].value.toLong,
          uptimeFields.get("unit").get.asInstanceOf[JsString].value
        ),
        fields.get("totalRequests").get.asInstanceOf[JsNumber].value.toLong,
        fields.get("openRequests").get.asInstanceOf[JsNumber].value.toLong,
        fields.get("maxOpenRequests").get.asInstanceOf[JsNumber].value.toLong,
        fields.get("totalConnections").get.asInstanceOf[JsNumber].value.toLong,
        fields.get("openConnections").get.asInstanceOf[JsNumber].value.toLong,
        fields.get("maxOpenConnections").get.asInstanceOf[JsNumber].value.toLong,
        fields.get("requestTimeouts").get.asInstanceOf[JsNumber].value.toLong
      )
    }
  }
}