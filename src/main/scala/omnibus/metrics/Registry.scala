package omnibus.metrics

import com.codahale.metrics._
import com.codahale.metrics.health.HealthCheckRegistry

import nl.grons.metrics.scala._

import omnibus.configuration._

object OmnibusRegistry {
  val metricRegistry = new MetricRegistry()
  val healthCheckRegistry = new HealthCheckRegistry()
}

trait Instrumented extends InstrumentedBuilder with CheckedBuilder {
  val metricRegistry = OmnibusRegistry.metricRegistry
  val registry = OmnibusRegistry.healthCheckRegistry
  override lazy val metricBaseName = MetricName(getClass)
}

trait InstrumentedActor extends ReceiveTimerActor with ReceiveExceptionMeterActor with Instrumented {
   override lazy val metricBaseName = MetricName("instrumentedActor")
}