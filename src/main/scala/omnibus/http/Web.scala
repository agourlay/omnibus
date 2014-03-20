package omnibus.http

import akka.io.IO
import spray.can.Http

import omnibus.configuration.Settings
import omnibus.core.{CoreActors, Core}

trait Web {
  this: Rest with CoreActors with Core =>

  val httpPort = Settings(system).Http.Port

  IO(Http)(system) ! Http.Bind(rootService, "localhost", port = 8080)
}