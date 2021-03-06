package omnibus.service.classic

import akka.actor.{ ActorRef, Props }

import omnibus.domain.topic._
import omnibus.domain.topic.TopicRepositoryProtocol._
import omnibus.service.classic.RootTopics.RootTopicsSet

class RootTopics(topicRepo: ActorRef) extends ClassicService {

  topicRepo ! TopicRepositoryProtocol.AllRoots

  override def receive = super.receive orElse waitingTopicsPathRef

  val roots = scala.collection.mutable.Set.empty[TopicView]

  def waitingTopicsPathRef: Receive = {
    case Roots(rootsPath) ⇒
      if (rootsPath.isEmpty) returnResult(RootTopicsSet(roots.toSet))
      else {
        rootsPath.foreach(_.topicRef.get ! TopicProtocol.View)
        context.become(super.receive orElse waitingTopicsView(rootsPath.size))
      }
  }

  def waitingTopicsView(expected: Integer): Receive = {
    case rootView: TopicView ⇒
      roots += rootView
      if (roots.size == expected) returnResult(RootTopicsSet(roots.toSet))
  }
}

object RootTopics {
  def props(topicRepo: ActorRef) = Props(classOf[RootTopics], topicRepo)
  case class RootTopicsSet(roots: Set[TopicView])
}
