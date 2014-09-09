package omnibus.api.streaming

import akka.actor.{ ActorSystem, Actor, Props, ActorLogging, ActorRef, ActorRefFactory }
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

class WebSocketResponse(val serverConnection: ActorRef, val coreActors : CoreActors) extends HttpServiceActor with websocket.WebSocketServerWorker with ActorLogging {

  override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

  override def handshaking: Receive = {
    case websocket.HandshakeRequest(state) =>
      state match {
        case wsFailure: websocket.HandshakeFailure => sender() ! wsFailure.response
        case wsContext: websocket.HandshakeContext => 
        setupSubscription(wsContext.request)
        sender() ! UHttp.UpgradeServer(websocket.pipelineStage(self, wsContext), wsContext.response)
      }

    case UHttp.Upgraded =>
      context.become(businessLogic orElse closeLogic)
      self ! websocket.UpgradedToWebSocket // notify Upgraded to WebSocket protocol
  }

  def setupSubscription(request: HttpRequest) {
    val path = request.uri.path
    val param = request.uri.query
    val ip = request.headers.filter(_.name == "Remote-Address").head.value
    log.info(s"info extracted $path $param $ip")
    // TODO extract new actor to setup things
    //val reactiveCmd = ReactiveCmd(ReactiveMode.withName(param.get("mode").getOrElse("simple")), None, None)
    //coreActors.subRepo ! SubscriberRepositoryProtocol.CreateSub(List(path.tail.toString), self, reactiveCmd, ip, SubscriberSupport.WS)
  }

  def businessLogic: Receive = {

    case TextFrame(content) ⇒
      send(TextFrame("You are not supposed to send me stuff"))

    case x: FrameCommandFailed =>
      log.error("frame command failed", x)

    case msg: Message => send(MessageObj.toMessageFrame(msg))
  }

  def businessLogicNoUpgrade: Receive = {
    implicit val refFactory: ActorRefFactory = context
    runRoute {
      complete("upgrade not available")
    }
  }
}

object WebSocketResponse {
  def props(serverConnection: ActorRef, coreActors : CoreActors) = Props(classOf[WebSocketResponse], serverConnection, coreActors)
}