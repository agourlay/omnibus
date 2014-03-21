package omnibus.api.endpoint

import akka.actor._
import akka.pattern.CircuitBreakerOpenException

import spray.util.LoggingContext
import spray.routing._
import spray.http._
import HttpHeaders._

import omnibus.core.CoreActors
import omnibus.api.route._
import omnibus.api.exceptions.RestFailureHandling
import omnibus.domain.topic._
import omnibus.domain.subscriber._
import omnibus.configuration._

class HttpEndpointActor(coreActors : CoreActors) extends HttpEndpoint with Actor {

  implicit def actorRefFactory = context    
  def receive = runRoute(routes(coreActors))

}

trait HttpEndpoint extends HttpService with RestFailureHandling{
 
   def routes(coreActors : CoreActors ) (implicit context: ActorContext) = {
   	  val subRepo = coreActors.subRepo
   	  val topicRepo = coreActors.topicRepo
   	  val httpStatService = coreActors.httpStatService

   	  val topicRoute = new TopicRoute(subRepo, topicRepo).route         // '/topics'
      val statsRoute = new StatsRoute(httpStatService, topicRepo).route // '/stats'
      val adminRoute = new AdminRoute(topicRepo, subRepo).route         // '/admin/topics'
      val adminUIRoute = new AdminUIRoute().route                       // '/ '
      val routes = topicRoute ~ statsRoute ~ adminRoute ~ adminUIRoute 
      routes
   }	
}

object HttpEndpointActor {
	def props(coreActors : CoreActors) = Props(classOf[HttpEndpointActor], coreActors)
}