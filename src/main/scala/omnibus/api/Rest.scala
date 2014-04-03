package omnibus.api

import akka.io.IO
import spray.can.Http

import omnibus.configuration._
import omnibus.api.endpoint.HttpEndpointActor
import omnibus.core.{CoreActors, Core}

trait Rest {
  this: CoreActors with Core =>

  val rootService = system.actorOf(HttpEndpointActor.props(this), "omnibus-http")  
}

trait Web {
  this: Rest with CoreActors with Core =>

  val httpPort = Settings(system).Http.Port

  IO(Http)(system) ! Http.Bind(rootService, "localhost", port = httpPort)
}