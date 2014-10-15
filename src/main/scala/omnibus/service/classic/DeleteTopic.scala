package omnibus.service.classic

import akka.actor._

import omnibus.domain.topic._
import omnibus.domain.topic.TopicRepositoryProtocol._

class DeleteTopic(topicPath: TopicPath, topicRepo: ActorRef) extends ClassicService {

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = super.receive orElse waitingLookup

  def waitingAck: Receive = {
    case t @ TopicDeletedFromRepo(topicPath) ⇒ context.parent forward t
  }

  def waitingLookup: Receive = {
    case TopicPathRef(topicPath, topicRef) ⇒
      topicRef match {
        case Some(ref) ⇒
          ref ! TopicProtocol.Delete
          topicRepo ! TopicRepositoryProtocol.DeleteTopic(topicPath)
          context.become(super.receive orElse waitingAck)
        case None ⇒ context.parent ! new TopicNotFoundException(topicPath.prettyStr)
      }
  }
}

object DeleteTopic {
  def props(topicPath: TopicPath, topicRepo: ActorRef) = Props(classOf[DeleteTopic], topicPath, topicRepo).withDispatcher("requests-dispatcher")
}