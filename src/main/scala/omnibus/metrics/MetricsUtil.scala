package omnibus.metrics

import nl.grons.metrics.scala._
import spray.json._
import DefaultJsonProtocol._
import MetricsJson._

object MetricsUtil {
  def magic(java: Any): JsValue = java match {
    case j: com.codahale.metrics.Timer      ⇒ fmtTimer.write(new Timer(j))
    case j: com.codahale.metrics.Gauge[Int] ⇒ fmtGauge.write(new Gauge(j))
    case j: com.codahale.metrics.Meter      ⇒ fmtMeter.write(new Meter(j))
    case j: com.codahale.metrics.Counter    ⇒ fmtCounter.write(new Counter(j))
    case _                                  ⇒ throw new RuntimeException("From java with love")
  }
}

object MetricsJson {

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

  implicit val fmtTimer: RootJsonFormat[Timer] = new RootJsonFormat[Timer] {
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