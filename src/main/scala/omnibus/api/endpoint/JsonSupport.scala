package omnibus.api.endpoint

import spray.json._
import DefaultJsonProtocol._

import nl.grons.metrics.scala._

import omnibus.domain.topic._
import omnibus.domain.subscriber._

object JsonSupport {

  implicit val formatTopicPath = new RootJsonFormat[TopicPath] {
    def write(obj: TopicPath) = JsObject(
      "topicPath" -> JsString(obj.prettyStr)
    )
    // we don't need to deserialize the TopicPath
    def read(json: JsValue): TopicPath = ???
  }

  implicit val formatTopicEvent = jsonFormat4(TopicEvent)

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
      "topic" -> JsArray(obj.topic.split("/").map(JsString(_)).toVector),
      "subTopicsNumber" -> JsNumber(obj.subTopicsNumber),
      "subscribersNumber" -> JsNumber(obj.subscribersNumber),
      "eventsNumber" -> JsNumber(obj.numEvents),
      "throughputPerSec" -> JsNumber(obj.throughputPerSec),
      "creationDate" -> JsNumber(obj.creationDate),
      "timestamp" -> JsNumber(obj.timestamp),
      "_embedded" -> JsObject("children" -> JsArray(
        obj.children.map(child ⇒ JsObject(child.split("/").last -> JsObject("href" -> JsString("/topics/" + child)))).toVector
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

  implicit val fmtCounter = new RootJsonFormat[Counter] {
    def write(obj: Counter) = JsObject(
      "count" -> JsNumber(obj.count)
    )
    // we don't need to deserialize
    def read(json: JsValue): Counter = ???
  }

  implicit val fmtGauge = new RootJsonFormat[Gauge[Int]] {
    def write(obj: Gauge[Int]) = JsObject(
      "count" -> JsNumber(obj.value)
    )
    // we don't need to deserialize
    def read(json: JsValue): Gauge[Int] = ???
  }

  implicit val fmtMeter = new RootJsonFormat[Meter] {
    def write(obj: Meter) = JsObject(
      "count" -> JsNumber(obj.count),
      "fifteenMinuteRate" -> JsNumber(obj.fifteenMinuteRate),
      "fiveMinuteRate" -> JsNumber(obj.fiveMinuteRate),
      "meanRate" -> JsNumber(obj.meanRate),
      "oneMinuteRate" -> JsNumber(obj.oneMinuteRate)
    )
    // we don't need to deserialize
    def read(json: JsValue): Meter = ???
  }

  implicit val fmtTimer = new RootJsonFormat[Timer] {
    def write(obj: Timer) = JsObject(
      "count" -> JsNumber(obj.count),
      "max" -> JsNumber(obj.max / 1000000),
      "min" -> JsNumber(obj.min / 1000000),
      "mean" -> JsNumber(obj.mean / 1000000),
      "stdDev" -> JsNumber(obj.stdDev / 1000000),
      "fifteenMinuteRate" -> JsNumber(obj.fifteenMinuteRate),
      "fiveMinuteRate" -> JsNumber(obj.fiveMinuteRate),
      "meanRate" -> JsNumber(obj.meanRate),
      "oneMinuteRate" -> JsNumber(obj.oneMinuteRate),
      "50p" -> JsNumber(obj.snapshot.getMedian() / 1000000),
      "75p" -> JsNumber(obj.snapshot.get75thPercentile() / 1000000),
      "95p" -> JsNumber(obj.snapshot.get95thPercentile() / 1000000),
      "98p" -> JsNumber(obj.snapshot.get98thPercentile() / 1000000),
      "99p" -> JsNumber(obj.snapshot.get99thPercentile() / 1000000),
      "999p" -> JsNumber(obj.snapshot.get999thPercentile() / 1000000)
    )
    // we don't need to deserialize
    def read(json: JsValue): Timer = ???
  }
}