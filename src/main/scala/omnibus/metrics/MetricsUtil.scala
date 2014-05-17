package omnibus.metrics

import nl.grons.metrics.scala._
import spray.json._
import DefaultJsonProtocol._
import MetricsJson._

// TODO This MUST go away...
object MetricsUtil {
	def magic(java : Any) : JsValue = {
		if (java.isInstanceOf[com.codahale.metrics.Timer]) {
			val timer = new Timer(java.asInstanceOf[com.codahale.metrics.Timer])
			return formatTimer.write(timer)
		}
		if (java.isInstanceOf[com.codahale.metrics.Meter]) {
			val meter = new Meter(java.asInstanceOf[com.codahale.metrics.Meter])
			return formatMeter.write(meter)
		}
		if (java.isInstanceOf[com.codahale.metrics.Counter]) {
			val counter = new Counter(java.asInstanceOf[com.codahale.metrics.Counter])
			return formatCounter.write(counter)
		}
		throw new RuntimeException("From java with love")
	}
}

object MetricsJson {

	implicit val formatCounter = new RootJsonFormat[Counter] {
	    def write(obj: Counter) = JsObject(
	      "count" -> JsNumber(obj.count)
	    )
	    // we don't need to deserialize the TopicPath
	    def read(json: JsValue): Counter = ???
    } 

	implicit val formatMeter = new RootJsonFormat[Meter] {
	    def write(obj: Meter) = JsObject(
	      "count"             -> JsNumber(obj.count),
	      "fifteenMinuteRate" -> JsNumber(obj.fifteenMinuteRate),
	      "fiveMinuteRate"    -> JsNumber(obj.fiveMinuteRate),
	      "meanRate"          -> JsNumber(obj.meanRate),
	      "oneMinuteRate"     -> JsNumber(obj.oneMinuteRate)
	    )
	    // we don't need to deserialize the TopicPath
	    def read(json: JsValue): Meter = ???
    } 

    implicit val formatTimer = new RootJsonFormat[Timer] {
	    def write(obj: Timer) = JsObject(
	      "count"             -> JsNumber(obj.count),
	      "max"               -> JsNumber(obj.max / 1000000),
	      "min"               -> JsNumber(obj.min / 1000000),
	      "mean"              -> JsNumber(obj.mean / 1000000),
	      "stdDev"            -> JsNumber(obj.stdDev / 1000000),
	      "fifteenMinuteRate" -> JsNumber(obj.fifteenMinuteRate),
	      "fiveMinuteRate"    -> JsNumber(obj.fiveMinuteRate),
	      "meanRate"          -> JsNumber(obj.meanRate),
	      "oneMinuteRate"     -> JsNumber(obj.oneMinuteRate)
	    )
	    // we don't need to deserialize the TopicPath
	    def read(json: JsValue): Timer = ???
    } 
}