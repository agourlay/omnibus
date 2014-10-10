package omnibus.domain.topic

import akka.actor._
import akka.persistence._

import scala.language.postfixOps

import omnibus.core.actors.CommonActor
import omnibus.configuration.Settings
import omnibus.domain.topic.TopicContentProtocol._

class TopicContent(val topicPath: TopicPath) extends PersistentActor with CommonActor {

  implicit def executionContext = context.dispatcher
  val system = context.system

  override def persistenceId = self.path.toStringWithoutAddress

  val timeout = akka.util.Timeout(Settings(context.system).Timeout)
  val retentionTime = Settings(system).Topic.RetentionTime
  var purgeScheduler: Cancellable = _

  override def preStart() = {
    purgeScheduler = system.scheduler.schedule(retentionTime, retentionTime, self, TopicContentProtocol.PurgeTopicContent)
    super.preStart()
  }

  val receiveRecover: Receive = {
    case _ ⇒ log.debug("no recovery write only model")
  }

  val receiveCommand: Receive = {
    case Publish(message, replyTo) ⇒ publishEvent(message, replyTo)
    case DeleteContent             ⇒ deleteTopicContent()
    case PurgeTopicContent         ⇒ purgeOldContent()
    case PurgeFrom(id)             ⇒ deleteMessages(id, true)
    case FwProcessorId(replyTo)    ⇒ replyTo ! TopicContentProtocol.ProcessorId(persistenceId)
  }

  override def postStop() = {
    purgeScheduler.cancel()
  }

  def purgeOldContent() {
    val timeLimit = System.currentTimeMillis - retentionTime.toMillis
    context.actorOf(TopicPurgerHelper.props(persistenceId, timeLimit), "purger-helper")
  }

  def deleteTopicContent() {
    if (lastSequenceNr != 0) {
      // erase all data from storage
      deleteMessages(lastSequenceNr)
      deleteSnapshots(SnapshotSelectionCriteria.Latest)
    }
    context stop self
  }

  def publishEvent(message: String, replyTo: ActorRef) = {
    val event = TopicEvent(lastSequenceNr + 1, topicPath, message)
    persistAsync(event) { evt ⇒
      context.parent ! TopicContentProtocol.Saved(replyTo)
    }
  }
}

object TopicContentProtocol {
  case class Publish(message: String, replyTo: ActorRef)
  case class Saved(replyTo: ActorRef)
  case class FwProcessorId(subscriber: ActorRef)
  case class ProcessorId(processorId: String)
  case class PurgeFrom(id: Long)
  case object DeleteContent
  case object PurgeTopicContent
}

object TopicContent {
  def props(topicPath: TopicPath) = Props(classOf[TopicContent], topicPath).withDispatcher("topics-dispatcher")
}