package omnibus.api.request

import akka.actor._

import spray.httpx.SprayJsonSupport._
import spray.routing._
import spray.json._

import DefaultJsonProtocol._

import omnibus.api.endpoint.JsonSupport._
import omnibus.domain.topic._
import omnibus.domain.topic.TopicRepositoryProtocol._

class RootTopics(ctx : RequestContext, topicRepo: ActorRef) extends RestRequest(ctx) {

  topicRepo ! TopicRepositoryProtocol.AllRoots

  override def receive = waitingTopicsPathRef orElse handleTimeout

  var roots = Set.empty[TopicView]

  def waitingTopicsPathRef : Receive = {
    case Roots(rootsPath) => {
      if (rootsPath.isEmpty){
        ctx.complete(roots)
        requestOver()
      } else {
        rootsPath.foreach (_.topicRef.get ! TopicProtocol.View)
        context.become(waitingTopicsView(rootsPath.size) orElse handleTimeout)
      }
    }  
  }

  def waitingTopicsView(expected : Integer) : Receive = {
    case rootView : TopicView => {
      roots += rootView
      if (roots.size == expected){
        ctx.complete(roots)
        requestOver()
      }
    }  
  }
}

object RootTopics {
   def props(ctx : RequestContext, topicRepo: ActorRef) 
     = Props(classOf[RootTopics], ctx, topicRepo).withDispatcher("requests-dispatcher")
}