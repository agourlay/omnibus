package omnibus.api

import akka.io.IO

import spray.can.server.UHttp
import spray.can.Http

import omnibus.configuration._
import omnibus.api.endpoint.{ApiEndpoint, WebSocketServer}
import omnibus.core.{CoreActors, Core}

trait Rest {
  this: CoreActors with Core =>

  val rootService = system.actorOf(ApiEndpoint.props(this), "omnibus-http")
  val webSocketServer = system.actorOf(WebSocketServer.props(this), "omnibus-websocket")  
}

trait Web {
  this: Rest with CoreActors with Core =>

  val httpPort = Settings(system).Http.Port
  val wsPort = Settings(system).Websocket.Port

  IO(Http)(system) ! Http.Bind(rootService, "localhost", port = httpPort)
  if (Settings(system).Websocket.Enable) IO(UHttp)(systemWS) ! Http.Bind(webSocketServer, "localhost", port = wsPort)
}