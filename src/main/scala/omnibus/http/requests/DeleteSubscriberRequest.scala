package omnibus.http.request

import akka.pattern._
import akka.actor._
import akka.actor.Status._

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.httpx.encoding._
import spray.routing._
import spray.routing.authentication._
import spray.json._
import spray.httpx.marshalling._
import spray.http._
import HttpHeaders._
import MediaTypes._

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.Future

import DefaultJsonProtocol._
import reflect.ClassTag

import omnibus.http.JsonSupport._
import omnibus.http.streaming._
import omnibus.configuration._
import omnibus.domain._
import omnibus.domain.topic._
import omnibus.domain.subscriber._
import omnibus.repository._

class DeleteSubscriberRequest(subId : String, ctx : RequestContext, subRepo: ActorRef) extends RestRequest(ctx) {

  subRepo ! SubscriberRepositoryProtocol.KillSub(subId)

  override def receive = waitingAck orElse handleTimeout

  def waitingAck : Receive = {
    case true        => {
     ctx.complete(StatusCodes.Accepted, s"Subscriber $subId deleted\n")
      self ! PoisonPill
    }  
    case Failure(ex) => {
      ctx.complete(ex)
      self ! PoisonPill
    }  
  }
}

object DeleteSubscriberRequest {
   def props(subId : String, ctx : RequestContext, subRepo: ActorRef) 
     = Props(classOf[DeleteSubscriberRequest],subId, ctx, subRepo)
}