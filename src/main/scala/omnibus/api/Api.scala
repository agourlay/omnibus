package omnibus.api

import akka.io.IO

import spray.can.server.UHttp
import spray.can.Http

import omnibus.configuration._
import omnibus.api.endpoint.ApiEndpoint
import omnibus.core.Core
import omnibus.core.actors.CoreActors
import omnibus.api.streaming.ws.WebSocketServer

trait Api {
  this: CoreActors with Core ⇒

  val httpPort = Settings(system).Http.Port
  val rootService = system.actorOf(ApiEndpoint.props(this), "omnibus-http")
  IO(Http)(system) ! Http.Bind(rootService, "0.0.0.0", port = httpPort)

  if (Settings(system).Websocket.Enable) {
    val wsPort = Settings(system).Websocket.Port
    val webSocketServer = system.actorOf(WebSocketServer.props(this), "omnibus-websocket")
    IO(UHttp)(systemWS) ! Http.Bind(webSocketServer, "0.0.0.0", port = wsPort)
  }
}
