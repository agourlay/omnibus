package omnibus.http

import akka.io.IO
import spray.can.Http

import omnibus.configuration._
import omnibus.core.{CoreActors, Core}


trait Rest {
  this: CoreActors with Core =>

  implicit def executionContext = system.dispatcher

  val httpPort = Settings(system).Http.Port

  // HttpService actor exposing omnibus routes
  val omnibusHttp = system.actorOf(HttpEndpoint.props(httpStatService, topicRepo, subRepo), "omnibus-http")  

  IO(Http) ! Http.Bind(omnibusHttp, "localhost", port = httpPort)

}