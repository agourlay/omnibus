package omnibus.service.classic

import akka.actor.{ Actor, ActorRef, Props }

import omnibus.domain.topic._
import omnibus.domain.topic.TopicProtocol._
import omnibus.domain.topic.TopicRepositoryProtocol._

class Publish(topicPath: TopicPath, message: String, topicRepo: ActorRef) extends ClassicService {

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = super.receive orElse waitingLookup

  def waitingAck: Receive = {
    case t @ MessagePublished ⇒ returnResult(t)
  }

  def waitingLookup: Receive = {
    case TopicPathRef(topicPath, topicRef) ⇒
      topicRef match {
        case Some(ref) ⇒
          ref ! TopicProtocol.PublishMessage(message)
          context.become(super.receive orElse waitingAck)
        case None ⇒ returnError(new TopicNotFoundException(topicPath.prettyStr))
      }
  }
}

object Publish {
  def props(topicPath: TopicPath, message: String, topicRepo: ActorRef) = Props(classOf[Publish], topicPath, message, topicRepo).withDispatcher("requests-dispatcher")
}