package omnibus.api.endpoint

import akka.actor._
import spray.can.Http

import omnibus.core.actors.CoreActors
import omnibus.api.streaming.ws.WebSocketResponse

class WebSocketServer(coreActors: CoreActors) extends Actor with ActorLogging {
  def receive = {
    case Http.Connected(remoteAddress, localAddress) ⇒
      val serverConnection = sender()
      val conn = context.actorOf(WebSocketResponse.props(serverConnection, coreActors))
      serverConnection ! Http.Register(conn)
  }
}

object WebSocketServer {
  def props(coreActors: CoreActors) = Props(classOf[WebSocketServer], coreActors)
}