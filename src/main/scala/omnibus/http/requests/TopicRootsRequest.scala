package omnibus.http.request

import akka.actor._

import spray.httpx.SprayJsonSupport._
import spray.routing._
import spray.json._
import spray.httpx.marshalling._
import spray.http._

import DefaultJsonProtocol._

import omnibus.http.JsonSupport._
import omnibus.domain.topic._
import omnibus.repository._

class TopicRootsRequest(ctx : RequestContext, topicRepo: ActorRef) extends RestRequest(ctx) {

  topicRepo ! TopicRepositoryProtocol.AllRoots

  override def receive = waitingTopicsPathRef orElse handleTimeout

  var roots : Set[TopicView] = Set.empty[TopicView]

  def waitingTopicsPathRef : Receive = {
    case rootsPath : List[TopicPathRef] => {
      if (!rootsPath.isEmpty){
        rootsPath.foreach (_.topicRef.get ! TopicProtocol.View)
        context.become(waitingTopicsView(rootsPath.size) orElse handleTimeout)
        } else {
          ctx.complete(roots)
          self ! PoisonPill
        }
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
     = Props(classOf[TopicRootsRequest], ctx, topicRepo).withDispatcher("requests-dispatcher")
}