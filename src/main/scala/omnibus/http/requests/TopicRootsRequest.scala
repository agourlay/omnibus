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

class TopicRootsRequest(ctx : RequestContext, topicRepo: ActorRef) extends RestRequest(ctx) {

  topicRepo ! TopicRepositoryProtocol.AllRoots

  override def receive = waitingTopicsPathRef orElse handleTimeout

  var roots : Set[TopicView] = Set.empty[TopicView]

  def waitingTopicsPathRef : Receive = {
    case rootsPath : List[TopicPathRef] => {
      rootsPath.foreach (_.topicRef.get ! TopicProtocol.View)
      context.become(waitingTopicsView(rootsPath.size) orElse handleTimeout)
    }  
  }

  def waitingTopicsView(expected : Integer) : Receive = {
    case rootView : TopicView => {
      roots += rootView
      if (roots.size == expected){
        ctx.complete(roots)
        self ! PoisonPill
      }
    }  
  }
}

object TopicRootsRequest {
   def props(ctx : RequestContext, topicRepo: ActorRef) 
     = Props(classOf[TopicRootsRequest], ctx, topicRepo)
}