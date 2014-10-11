package omnibus.service.streamed

import akka.actor.{ Actor, ActorRef, Props }

import omnibus.domain.topic._
import omnibus.domain.topic.TopicRepositoryProtocol._

class StreamTopicLeaves(replyTo: ActorRef, topicRepo: ActorRef) extends StreamedService(replyTo) {

  topicRepo ! TopicRepositoryProtocol.AllRoots

  override def receive = receiveRoots orElse super.receive

  def receiveRoots: Receive = {
    case Roots(rootsPath) ⇒
      if (rootsPath.isEmpty) replyTo ! EndOfStream
      else rootsPath.foreach(_.topicRef.get ! TopicProtocol.Leaves(replyTo))
  }
}

object StreamTopicLeaves {
  def props(replyTo: ActorRef, topicRepo: ActorRef) = Props(classOf[StreamTopicLeaves], replyTo, topicRepo).withDispatcher("streaming-dispatcher")
}