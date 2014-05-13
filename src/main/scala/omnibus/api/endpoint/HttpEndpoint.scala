package omnibus.api.endpoint

import akka.actor._

import spray.routing._

import omnibus.core.CoreActors
import omnibus.api.route._
import omnibus.api.exceptions.RestFailureHandling

class HttpEndpointActor(coreActors : CoreActors) extends HttpEndpoint with Actor {
  implicit def actorRefFactory = context    
  def receive = runRoute(routes(coreActors))
}

trait HttpEndpoint extends HttpService with RestFailureHandling {
 
  def routes(coreActors : CoreActors ) (implicit context: ActorContext) = {
    val subRepo = coreActors.subRepo
    val topicRepo = coreActors.topicRepo
    val metricsRepo = coreActors.metricsReporter

    val topicRoute = new TopicRoute(subRepo, topicRepo).route     // '/topics'
    val statsRoute = new StatsRoute(topicRepo, metricsRepo).route // '/stats'
    val adminRoute = new AdminRoute(topicRepo, subRepo).route     // '/admin/topics'
    val adminUIRoute = new AdminUIRoute().route                   // '/ '
    topicRoute ~ statsRoute ~ adminRoute ~ adminUIRoute 
  }	
}

object HttpEndpointActor {
	def props(coreActors : CoreActors) = Props(classOf[HttpEndpointActor], coreActors)
}