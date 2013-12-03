package omnibus.domain

import akka.actor._
import akka.pattern._
import spray.routing._
import spray.http._
import spray.http.MediaTypes._
import HttpHeaders._
import spray.can.Http
import spray.can.server.Stats
import scala.language.postfixOps

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.collection.mutable.ListBuffer

import omnibus.domain.JsonSupport._
import omnibus.util._
import omnibus.domain.HttpSubscribeProtocol._


class HttpSubscriber(responder: ActorRef) extends StreamingResponse(responder) {

  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(3 seconds)	
  
  var subscriptions : ListBuffer[String] = ListBuffer.empty[String]

  override def startText = "Streaming subscription...\n"

  override def receive = {
    case message : Message              => pushMessage(message)
    case AcknowledgeSubscription(topic) => ackSubscription(topic)
    case _                              => super.receive   
  }

  def pushMessage(message : Message) = {
    val nextChunk = MessageChunk("data: "+ formatMessage.write(message) +"\n\n")
    responder ! nextChunk 
  }

  def ackSubscription(topicName : String) = subscriptions += topicName

}

object HttpSubscribeProtocol {
  case class AcknowledgeSubscription(topicName : String)
}