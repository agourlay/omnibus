package omnibus.service.classic

import akka.actor.{ ActorRef, Props }

import omnibus.core.metrics.MetricsReporterProtocol._
import omnibus.core.metrics.MetricsReporterProtocol

class AllMetrics(metricsRepo: ActorRef) extends ClassicService {

  metricsRepo ! MetricsReporterProtocol.All

  override def receive = super.receive orElse waitingMetrics

  def waitingMetrics: Receive = {
    case m @ MetricsReport(metrics) ⇒ returnResult(m)
  }
}

object AllMetrics {
  def props(metricsRepo: ActorRef) = Props(classOf[AllMetrics], metricsRepo)
}