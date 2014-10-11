package omnibus.service.streamed

import akka.actor._

import omnibus.domain.topic._
import omnibus.domain.topic.TopicRepositoryProtocol._
import omnibus.configuration._

class StreamTopicLeaves(replyTo: ActorRef, topicRepo: ActorRef) extends StreamedService(replyTo) {

  implicit def system = context.system
  implicit val timeout = akka.util.Timeout(Settings(system).Timeout)

  context.setReceiveTimeout(timeout.duration)

  topicRepo ! TopicRepositoryProtocol.AllRoots

  override def receive: Receive = {
    case ReceiveTimeout ⇒
      // getting the leaves is a stream service within a bounded timeframe
      // i.e. you will not receive leaves created after the request
      replyTo ! EndOfStream
      self ! PoisonPill
    case Roots(rootsPath) ⇒
      if (rootsPath.isEmpty) replyTo ! EndOfStream
      else rootsPath.foreach(_.topicRef.get ! TopicProtocol.Leaves(replyTo))
  }
}

object StreamTopicLeaves {
  def props(replyTo: ActorRef, topicRepo: ActorRef) = Props(classOf[StreamTopicLeaves], replyTo, topicRepo).withDispatcher("streaming-dispatcher")
}