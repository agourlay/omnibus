package omnibus.api.endpoint

import akka.actor._

import spray.routing._

import omnibus.core.actors.CoreActors
import omnibus.api.route._
import omnibus.api.exceptions.RestFailureHandling

class ApiEndpoint(coreActors: CoreActors) extends HttpEndpoint with Actor {
  implicit def actorRefFactory = context
  def receive = runRoute(routes(coreActors))
}

trait HttpEndpoint extends HttpService with RestFailureHandling {

  def routes(core: CoreActors)(implicit context: ActorContext) = {

    // '/topics'
    val topicRoute = new TopicRoute(core.subRepo, core.topicRepo).route
    // '/stats'
    val statsRoute = new StatsRoute(core.topicRepo, core.metricsReporter).route
    // '/admin/topics'
    val adminRoute = new AdminRoute(core.topicRepo, core.subRepo).route
    // '/ '
    val staticFilesRoute = new StaticFilesRoute().route

    topicRoute ~ statsRoute ~ adminRoute ~ staticFilesRoute
  }
}

object ApiEndpoint {
  def props(coreActors: CoreActors) = Props(classOf[ApiEndpoint], coreActors)
}