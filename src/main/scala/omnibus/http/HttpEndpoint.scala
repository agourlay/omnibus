package omnibus.http

import akka.actor._
import akka.pattern.CircuitBreakerOpenException

import spray.util.LoggingContext
import spray.routing._
import spray.http._
import HttpHeaders._

import omnibus.http.route._
import omnibus.domain.topic._
import omnibus.domain.subscriber._
import omnibus.configuration._

class HttpEndpoint(httpStatService : ActorRef, topicRepo : ActorRef, subRepo : ActorRef) extends HttpService with RestFailureHandling with Actor {

  implicit def actorRefFactory = context

  val topicRoute = new TopicRoute(subRepo, topicRepo).route         // '/topics'
  val statsRoute = new StatsRoute(httpStatService, topicRepo).route // '/stats'
  val adminRoute = new AdminRoute(topicRepo, subRepo).route         // '/admin/topics'
  val adminUIRoute = new AdminUIRoute().route                       // '/ '

  val routes = topicRoute ~ statsRoute ~ adminRoute ~ adminUIRoute         
                                  
  def receive = runRoute(routes)

}

object HttpEndpoint {
	def props(httpStatService : ActorRef, topicRepo : ActorRef, subRepo : ActorRef)
    = Props(classOf[HttpEndpoint], httpStatService, topicRepo, subRepo)
}