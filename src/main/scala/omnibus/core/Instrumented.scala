package omnibus.core

import akka.actor._

import java.util.concurrent.TimeUnit
import java.net.InetSocketAddress

import com.codahale.metrics._
import com.codahale.metrics.graphite._

import nl.grons.metrics.scala._

import omnibus.configuration._

object OmnibusRegistry {
  val metricRegistry = new MetricRegistry()
}

trait Instrumented extends InstrumentedBuilder {
  val metricRegistry = OmnibusRegistry.metricRegistry
}

trait InstrumentedActor extends ReceiveTimerActor 
                           with ReceiveCounterActor
                           with ReceiveExceptionMeterActor
                           with Instrumented

class MetricsReporter extends Actor with ActorLogging with Instrumented {

	val system = context.system

	val jmxReporter = JmxReporter.forRegistry(metricRegistry).build()
  jmxReporter.start()

  log.info(s"Starting MetricsReporter to JMX")

  if (Settings(system).Graphite.Enable) {
  	val graphiteHost = Settings(system).Graphite.Host
  	log.info(s"Starting MetricsReporter to Graphite $graphiteHost:2003")

   	val graphite = new Graphite(new InetSocketAddress(graphiteHost, 2003))
    val graphiteReporter = GraphiteReporter.forRegistry(metricRegistry)
                                           .prefixedWith(Settings(system).Graphite.Prefix)
                                           .convertRatesTo(TimeUnit.SECONDS)
                                           .convertDurationsTo(TimeUnit.MILLISECONDS)
                                           .filter(MetricFilter.ALL)
  
                                           .build(graphite)
    graphiteReporter.start(1, TimeUnit.MINUTES)
  } 

	def receive = {
		case m : Any => log.debug("Don't talk to me")
	}
}

object MetricsReporter {  
  def props() = Props(classOf[MetricsReporter]).withDispatcher("statistics-dispatcher")
}