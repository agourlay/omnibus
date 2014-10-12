package omnibus.api

import akka.io.IO

import spray.can.server.UHttp
import spray.can.Http

import omnibus.api.endpoint.ApiEndpoint
import omnibus.api.streaming.ws.WebSocketServer
import omnibus.configuration._
import omnibus.core.Core
import omnibus.core.actors.CoreActors

trait Api {
  this: CoreActors with Core ⇒

  val rootService = system.actorOf(ApiEndpoint.props(this), "omnibus-http")
  IO(Http)(system) ! Http.Bind(rootService, "0.0.0.0", port = Settings(system).Http.Port)

  if (Settings(system).Websocket.Enable) {
    val webSocketServer = system.actorOf(WebSocketServer.props(this), "omnibus-websocket")
    IO(UHttp)(systemWS) ! Http.Bind(webSocketServer, "0.0.0.0", port = Settings(system).Websocket.Port)
  }
}
