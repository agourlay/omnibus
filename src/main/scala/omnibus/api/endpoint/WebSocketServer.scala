package omnibus.api.endpoint

import akka.actor._
import spray.can.Http

import omnibus.api.streaming.WebSocketResponse

class WebSocketServer extends Actor with ActorLogging {
  def receive = {
    case Http.Connected(remoteAddress, localAddress) =>
      val serverConnection = sender()
      val conn = context.actorOf(WebSocketResponse.props(serverConnection))
      serverConnection ! Http.Register(conn)
  }
}

object WebSocketServer {
  def props() = Props(classOf[WebSocketServer])
}