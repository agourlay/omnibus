package omnibus.service.classic

import akka.actor.{ ActorRef, Props }

import omnibus.domain.topic._
import omnibus.domain.topic.TopicProtocol._

class CreateTopic(topicPath: TopicPath, topicRepo: ActorRef) extends ClassicService {

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = super.receive orElse waitingLookup

  def waitingAck: Receive = {
    case t @ TopicCreated(topicRef) ⇒ returnResult(t)
  }

  def waitingLookup: Receive = {
    case TopicPathRef(topicPath, topicRef) ⇒ topicRef match {
      case Some(ref) ⇒ returnError(new TopicAlreadyExistsException(topicPath.prettyStr()))
      case None ⇒
        topicRepo ! TopicRepositoryProtocol.CreateTopic(topicPath)
        context.become(super.receive orElse waitingAck)
    }
  }
}

object CreateTopic {
  def props(topicPath: TopicPath, topicRepo: ActorRef) = Props(classOf[CreateTopic], topicPath, topicRepo)
}