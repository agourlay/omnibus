package omnibus.core.metrics

import akka.actor.Props

import java.util.concurrent.TimeUnit

import scala.collection.JavaConversions._

import spray.json.JsValue

import com.codahale.metrics.JmxReporter
import com.codahale.metrics.MetricFilter
import com.codahale.metrics.{ Counter ⇒ JCounter }
import com.codahale.metrics.{ Meter ⇒ JMeter }
import com.codahale.metrics.{ Timer ⇒ JTimer }
import com.codahale.metrics.{ Gauge ⇒ JGauge }
import com.codahale.metrics.graphite._

import nl.grons.metrics.scala._

import omnibus.configuration._
import omnibus.core.actors.CommonActor
import omnibus.core.metrics.MetricsReporterProtocol._
import omnibus.api.endpoint.JsonSupport._

class MetricsReporter extends CommonActor {

  JmxReporter.forRegistry(metricRegistry).build().start()

  log.info("Starting MetricsReporter to JMX")

  if (Settings(context.system).Graphite.Enable) {
    val graphiteHost = Settings(context.system).Graphite.Host
    val graphitePort = Settings(context.system).Graphite.Port

    log.info(s"Starting MetricsReporter to Graphite $graphiteHost:$graphitePort")

    val graphite = new Graphite(graphiteHost, graphitePort)
    val graphiteReporter = GraphiteReporter.forRegistry(metricRegistry)
      .prefixedWith(Settings(context.system).Graphite.Prefix)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .filter(MetricFilter.ALL)
      .build(graphite)

    graphiteReporter.start(1, TimeUnit.MINUTES)
  }

  def receive = {
    case All           ⇒ sender ! metricsByName(MetricsReporter.allMetrics)
    case Api           ⇒ sender ! metricsByName(MetricsReporter.apiMetrics)
    case Service       ⇒ sender ! metricsByName(MetricsReporter.serviceMetrics)
    case Topics        ⇒ sender ! metricsByName(MetricsReporter.topicMetrics)
    case Subscriptions ⇒ sender ! metricsByName(MetricsReporter.subRepoMetrics)
  }

  def metricsByName(name: String) = {
    val rawMap = metricRegistry.getMetrics.filterKeys(_.contains(name))
    MetricsReport(rawMap.toMap.mapValues(toJsValue(_)))
  }

  def toJsValue(java: Any): JsValue = java match {
    case j: JTimer      ⇒ fmtTimer.write(new Timer(j))
    case j: JGauge[Int] ⇒ fmtGauge.write(new Gauge(j))
    case j: JMeter      ⇒ fmtMeter.write(new Meter(j))
    case j: JCounter    ⇒ fmtCounter.write(new Counter(j))
    case _              ⇒ throw new RuntimeException("From java with love")
  }
}

object MetricsReporter {
  def props = Props(classOf[MetricsReporter])
  val allMetrics = "omnibus"
  val apiMetrics = "omnibus.api"
  val serviceMetrics = "omnibus.service"
  val topicMetrics = "omnibus.domain.topic"
  val subRepoMetrics = "omnibus.domain.subscriber"
}

object MetricsReporterProtocol {
  case object All
  case object Api
  case object Service
  case object Topics
  case object Subscriptions
  case class MetricsReport(metrics: Map[String, JsValue])
}