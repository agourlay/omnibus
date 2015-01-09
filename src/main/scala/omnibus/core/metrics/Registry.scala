package omnibus.core.metrics

import com.codahale.metrics._
import com.codahale.metrics.health.HealthCheckRegistry

import nl.grons.metrics.scala._

object OmnibusRegistry {
  val metricRegistry = new MetricRegistry()
  val healthCheckRegistry = new HealthCheckRegistry()
}

trait Instrumented extends InstrumentedBuilder with CheckedBuilder with FutureMetrics {
  val metricRegistry = OmnibusRegistry.metricRegistry
  val registry = OmnibusRegistry.healthCheckRegistry
  override lazy val metricBaseName = MetricName(getClass)
}