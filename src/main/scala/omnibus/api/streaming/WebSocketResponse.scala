package omnibus.api.streaming

import akka.actor.{ ActorSystem, Actor, Props, ActorLogging, ActorRef, ActorRefFactory }
import akka.io.IO
import spray.can.Http
import spray.can.server.UHttp
import spray.can.websocket
import spray.can.websocket.frame.{ BinaryFrame, TextFrame }
import spray.http.HttpRequest
import spray.can.websocket.FrameCommandFailed
import spray.routing.HttpServiceActor

class WebSocketResponse(val serverConnection: ActorRef) extends HttpServiceActor with websocket.WebSocketServerWorker with ActorLogging {

  override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

  def businessLogic: Receive = {

    case TextFrame(content) ⇒
      val strContent = content.decodeString("UTF-8")
      log.info(s"Content: $strContent")

    case x: FrameCommandFailed =>
      log.error("frame command failed", x)

    case Push(msg) => send(TextFrame(msg))
  }

  def businessLogicNoUpgrade: Receive = {
    implicit val refFactory: ActorRefFactory = context
    runRoute {
      complete("upgrade not available")
    }
  }
}

object WebSocketResponse {
  def props(serverConnection: ActorRef) = Props(classOf[WebSocketResponse], serverConnection)
}

final case class Push(msg: String)