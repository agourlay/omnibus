package omnibus.metrics

import nl.grons.metrics.scala._
import spray.json._
import omnibus.api.endpoint.JsonSupport._

object MetricsUtil {
  def magic(java: Any): JsValue = java match {
    case j: com.codahale.metrics.Timer      ⇒ fmtTimer.write(new Timer(j))
    case j: com.codahale.metrics.Gauge[Int] ⇒ fmtGauge.write(new Gauge(j))
    case j: com.codahale.metrics.Meter      ⇒ fmtMeter.write(new Meter(j))
    case j: com.codahale.metrics.Counter    ⇒ fmtCounter.write(new Counter(j))
    case _                                  ⇒ throw new RuntimeException("From java with love")
  }
}