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

class AllSubscribersRequest(ctx : RequestContext, subRepo: ActorRef) extends RestRequest(ctx) {

  subRepo ! SubscriberRepositoryProtocol.AllSubs

  override def receive = waitingLookup orElse handleTimeout

  def waitingLookup : Receive = {
    case subs : List[SubscriberView] => {
      ctx.complete(subs)
      self ! PoisonPill
    }  
  }
}

object AllSubscribersRequest {
   def props(ctx : RequestContext, subRepo: ActorRef) 
     = Props(classOf[AllSubscribersRequest], ctx, subRepo)
}