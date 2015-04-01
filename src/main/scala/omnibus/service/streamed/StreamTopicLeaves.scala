package omnibus.service.streamed

import akka.actor._

import omnibus.domain.topic._
import omnibus.domain.topic.TopicRepositoryProtocol._
import omnibus.configuration._

class StreamTopicLeaves(topicRepo: ActorRef) extends StreamedService {

  implicit val system = context.system
  implicit val timeout = akka.util.Timeout(Settings(system).Timeout)

  context.setReceiveTimeout(timeout.duration)

  topicRepo ! TopicRepositoryProtocol.AllRoots

  override def receive: Receive = {
    case ReceiveTimeout ⇒
      // getting the leaves is a stream service within a bounded time window
      // i.e. you will not receive leaves created after the request
      context.parent ! EndOfStream
      self ! PoisonPill
    case Roots(rootsPath) ⇒
      if (rootsPath.isEmpty) context.parent ! EndOfStream
      else rootsPath.foreach(_.topicRef.get ! TopicProtocol.Leaves(context.parent))
  }
}

object StreamTopicLeaves {
  def props(topicRepo: ActorRef) = Props(classOf[StreamTopicLeaves], topicRepo)
}