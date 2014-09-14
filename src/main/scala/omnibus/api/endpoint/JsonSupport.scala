package omnibus.api.endpoint

import spray.json._
import DefaultJsonProtocol._

import omnibus.domain.message._
import omnibus.domain.topic._
import omnibus.domain.subscriber._

object JsonSupport {

  implicit val formatTopicPath = new RootJsonFormat[TopicPath] {
    def write(obj: TopicPath) = JsObject(
      "topicPath" -> JsString(obj.prettyStr())
    )
    // we don't need to deserialize the TopicPath
    def read(json: JsValue): TopicPath = ???
  }

  implicit val formatMessage = jsonFormat4(Message)

  implicit val formatSubView = new RootJsonFormat[SubscriberView] {
    def write(obj: SubscriberView) = JsObject(
      "topic" -> JsString(obj.topic),
      "id" -> JsString(obj.id),
      "ip" -> JsString(obj.ip),
      "mode" -> JsString(obj.mode),
      "support" -> JsString(obj.support),
      "creationDate" -> JsNumber(obj.creationDate)
    )

    // we don't need to deserialize the SubscriberView
    def read(json: JsValue): SubscriberView = ???
  }

  implicit val formatTopicView = new RootJsonFormat[TopicView] {
    def write(obj: TopicView) = JsObject(
      "topic" -> JsArray(obj.topic.split("/").map(JsString(_)).toList),
      "subTopicsNumber" -> JsNumber(obj.subTopicsNumber),
      "subscribersNumber" -> JsNumber(obj.subscribersNumber),
      "eventsNumber" -> JsNumber(obj.numEvents),
      "throughputPerSec" -> JsNumber(obj.throughputPerSec),
      "creationDate" -> JsNumber(obj.creationDate),
      "timestamp" -> JsNumber(obj.timestamp),
      "_embedded" -> JsObject("children" -> JsArray(
        obj.children.map(child => JsObject(child.split("/").last -> JsObject("href" -> JsString("/topics/" + child)))).toList
      )
      ),
      "_links" -> JsArray(
        JsObject("self" -> JsObject("href" -> JsString("/topics/" + obj.topic))),
        JsObject("subscribe" -> JsObject("href" -> JsString("/streams/topics/" + obj.topic))),
        JsObject("stats" -> JsObject("href" -> JsString("/stats/topics/" + obj.topic)))
      )
    )

    // we don't need to deserialize the view
    def read(json: JsValue): TopicView = ???
  }
}