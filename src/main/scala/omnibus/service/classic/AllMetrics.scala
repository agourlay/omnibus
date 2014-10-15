package omnibus.service.classic

import akka.actor._

import omnibus.core.metrics.MetricsReporter._
import omnibus.core.metrics.MetricsReporterProtocol._
import omnibus.core.metrics.MetricsReporterProtocol

class AllMetrics(metricsRepo: ActorRef) extends ClassicService {

  metricsRepo ! MetricsReporterProtocol.All

  override def receive = super.receive orElse waitingMetrics

  def waitingMetrics: Receive = {
    case m @ MetricsReport(metrics) ⇒ context.parent forward m
  }
}

object AllMetrics {
  def props(metricsRepo: ActorRef) = Props(classOf[AllMetrics], metricsRepo).withDispatcher("requests-dispatcher")
}