package omnibus.http

import omnibus.configuration._
import omnibus.core.{CoreActors, Core}

trait Rest {
  this: CoreActors with Core =>

  implicit def executionContext = system.dispatcher

  // HttpService actor exposing omnibus routes
  val rootService = system.actorOf(HttpEndpoint.props(httpStatService, topicRepo, subRepo), "omnibus-http")  
}