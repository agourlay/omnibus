package omnibus.service.classic

import akka.actor.{ Actor, ActorRef, Props }

import omnibus.domain.topic._
import omnibus.domain.topic.TopicRepositoryProtocol._

class ViewTopic(topicPath: TopicPath, topicRepo: ActorRef) extends ClassicService {

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = super.receive orElse waitingLookup

  def waitingTopicView: Receive = {
    case tv: TopicView ⇒ returnResult(tv)
  }

  def waitingLookup: Receive = {
    case TopicPathRef(topicPath, topicRef) ⇒
      topicRef match {
        case Some(ref) ⇒
          ref ! TopicProtocol.View
          context.become(super.receive orElse waitingTopicView)
        case None ⇒ returnError(new TopicNotFoundException(topicPath.prettyStr))
      }
  }
}

object ViewTopic {
  def props(topicPath: TopicPath, topicRepo: ActorRef) = Props(classOf[ViewTopic], topicPath, topicRepo)
}