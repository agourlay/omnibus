package omnibus.api.request

import akka.actor._

import spray.httpx.SprayJsonSupport._
import spray.routing._
import spray.json._

import DefaultJsonProtocol._

import omnibus.api.endpoint.JsonSupport._
import omnibus.metrics.MetricsReporter._
import omnibus.metrics.MetricsReporterProtocol._
import omnibus.metrics.MetricsReporterProtocol

class AllMetrics(ctx: RequestContext, metricsRepo: ActorRef) extends RestRequest(ctx) {

  metricsRepo ! MetricsReporterProtocol.All

  override def receive = super.receive orElse waitingMetrics

  def waitingMetrics: Receive = {
    case MetricsReport(metrics) => requestOver(metrics)
  }
}

object AllMetrics {
  def props(ctx: RequestContext, metricsRepo: ActorRef) = Props(classOf[AllMetrics], ctx, metricsRepo).withDispatcher("requests-dispatcher")
}