package omnibus.api.streaming

import akka.actor._
import akka.io.IO

import spray.can.Http
import spray.can.server.UHttp
import spray.can.websocket
import spray.can.websocket.frame.{ BinaryFrame, TextFrame }
import spray.http.HttpRequest
import spray.http.Uri.Path
import spray.http.Uri.Query
import spray.can.websocket.FrameCommandFailed
import spray.routing.HttpServiceActor

import omnibus.core.CoreActors
import omnibus.domain.message._
import omnibus.domain.subscriber._
import omnibus.domain.subscriber.SubscriberRepositoryProtocol._
import omnibus.domain.topic._

class WebSocketResponse(val serverConnection: ActorRef, val coreActors: CoreActors) extends HttpServiceActor with websocket.WebSocketServerWorker with ActorLogging {

  override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

  override def handshaking: Receive = {
    case websocket.HandshakeRequest(state) =>
      state match {
        case wsFailure: websocket.HandshakeFailure => sender() ! wsFailure.response
        case wsContext: websocket.HandshakeContext =>
          sender() ! UHttp.UpgradeServer(websocket.pipelineStage(self, wsContext), wsContext.response)
          routing(wsContext.request)
      }

    case UHttp.Upgraded =>
      context.become(businessLogic orElse closeLogic)
      self ! websocket.UpgradedToWebSocket // notify Upgraded to WebSocket protocol
  }

  def routing(request: HttpRequest) {
    val path = request.uri.path
    val param = request.uri.query
    val ip = request.headers.filter(_.name == "Remote-Address").head.value
    log.info(s"Incoming websocket request $path $param from $ip")
    // TODO clean that for better routing
    if (path.toString.startsWith("/streams/topics/")) {
      val reactiveCmd = ReactiveCmd(ReactiveMode.withName(param.get("react").getOrElse("simple")), None, None)
      context.actorOf(WebSocketTopicSubscriber.props(TopicPath(path.tail.toString.split("/")(2)), reactiveCmd, ip, coreActors.subRepo, coreActors.topicRepo))
    }
  }

  def businessLogic: Receive = {

    case TextFrame(content) ⇒
      send(TextFrame("You are not supposed to send me stuff"))

    case x: FrameCommandFailed =>
      log.error("frame command failed", x)

    case msg: Message =>
      send(MessageObj.toMessageFrame(msg))

    case e: Exception =>
      send(TextFrame(e.getMessage))
      context.stop(self)
  }

  def businessLogicNoUpgrade: Receive = {
    implicit val refFactory: ActorRefFactory = context
    runRoute {
      complete("upgrade not available")
    }
  }
}

object WebSocketResponse {
  def props(serverConnection: ActorRef, coreActors: CoreActors) = Props(classOf[WebSocketResponse], serverConnection, coreActors)
}