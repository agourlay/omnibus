package omnibus.http

import java.util.Date
import java.text.SimpleDateFormat
import java.text.ParseException
import java.util.concurrent.TimeUnit

import scala.concurrent.duration._

import spray.json._
import spray.http.Uri
import DefaultJsonProtocol._

import omnibus.domain._
import omnibus.domain.topic._
import omnibus.domain.subscriber._
import omnibus.http.stats.HttpStats

object JsonSupport {

  implicit val formatTopicPath = new RootJsonFormat[TopicPath] {
    def write(obj: TopicPath): JsValue = JsObject(
      "topicPath"           -> JsString(obj.prettyStr())
    )
    // we don't need to deserialize the TopicPath
    def read(json: JsValue): TopicPath = ???
  }  

  implicit val formatMessage = jsonFormat4(Message)

  implicit val formatSubView = new RootJsonFormat[SubscriberView] {
    def write(obj: SubscriberView): JsValue = JsObject(
      "topic"        -> JsString(obj.topic),
      "id"           -> JsString(obj.id),
      "ip"           -> JsString(obj.ip),
      "creationDate" -> JsNumber(obj.creationDate)
    )

    // we don't need to deserialize the SubscriberView
    def read(json: JsValue): SubscriberView = ???
  }  

  implicit val formatTopicStats = new RootJsonFormat[TopicStatisticValue] {
    def write(obj: TopicStatisticValue): JsValue = JsObject(
      "topic"              -> JsString(obj.topic),
      "subTopicsNumber"    -> JsNumber(obj.subTopicsNumber),
      "subscribersNumber"  -> JsNumber(obj.subscribersNumber),
      "throughputPerSec"   -> JsNumber(obj.throughputPerSec),
      "timestamp"          -> JsNumber(obj.timestamp)
    )

    // we don't need to deserialize the TopicStatisticValue
    def read(json: JsValue): TopicStatisticValue = ???
  }  

  implicit val formatTopicView = new RootJsonFormat[TopicView] {
    def write(obj: TopicView): JsValue = JsObject(
      "topic"              -> JsArray(obj.topic.split("/").map(JsString(_)).toList),
      "subTopicsNumber"    -> JsNumber(obj.subTopicsNumber),
      "subscribersNumber"  -> JsNumber(obj.subscribersNumber),
      "eventsNumber"       -> JsNumber(obj.numEvents),
      "creationDate"       -> JsNumber(obj.creationDate),
      "viewDate"           -> JsNumber(obj.viewDate),  
      "_embedded"          -> JsObject("children" -> JsArray(
        obj.children.map( child => JsObject( child.split("/").last ->  JsObject("href" -> JsString("/topics/"+child)))).toList
        )
      ),
      "_links"             -> JsArray(
        JsObject("self"      -> JsObject("href" -> JsString("/topics/"+obj.topic))),
        JsObject("subscribe" -> JsObject("href" -> JsString("/streams/topics/"+obj.topic))),
        JsObject("stats"     -> JsObject("href" -> JsString("/stats/topics/"+obj.topic)))
      )
    )

    // we don't need to deserialize the view
    def read(json: JsValue): TopicView = ???
  }  

  implicit val formatHttpServerStats = new RootJsonFormat[HttpStats] {
    def write(obj: HttpStats): JsValue = JsObject(
      "uptimeInMilli"      -> JsNumber(obj.uptimeInMilli),
      "totalRequests"      -> JsNumber(obj.totalRequests),
      "openRequests"       -> JsNumber(obj.openRequests),
      "maxOpenRequests"    -> JsNumber(obj.maxOpenRequests),
      "totalConnections"   -> JsNumber(obj.totalConnections),
      "openConnections"    -> JsNumber(obj.openConnections),
      "maxOpenConnections" -> JsNumber(obj.maxOpenConnections),
      "requestTimeouts"    -> JsNumber(obj.requestTimeouts),
      "timestamp"          -> JsNumber(obj.timestamp)
    )

    // we don't need to deserialize
    def read(json: JsValue): HttpStats = ???
  }
}