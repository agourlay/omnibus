package omnibus.service.classic

import akka.actor.{ ActorRef, Props }

import omnibus.domain.topic._
import omnibus.domain.topic.TopicRepositoryProtocol._

class DeleteTopic(topicPath: TopicPath, topicRepo: ActorRef) extends ClassicService {

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = super.receive orElse waitingLookup

  def waitingAck: Receive = {
    case t @ TopicDeletedFromRepo(_) ⇒ returnResult(t)
  }

  def waitingLookup: Receive = {
    case TopicPathRef(path, topicRef) ⇒
      topicRef match {
        case Some(ref) ⇒
          ref ! TopicProtocol.Delete
          topicRepo ! TopicRepositoryProtocol.DeleteTopic(path)
          context.become(super.receive orElse waitingAck)
        case None ⇒ returnError(new TopicNotFoundException(path.prettyStr))
      }
  }
}

object DeleteTopic {
  def props(topicPath: TopicPath, topicRepo: ActorRef) = Props(classOf[DeleteTopic], topicPath, topicRepo)
}