package omnibus.domain.topic

import akka.actor._
import akka.persistence._

import scala.language.postfixOps

import omnibus.configuration.Settings
import omnibus.domain.message._
import omnibus.domain.topic.TopicContentProtocol._

class TopicContent(val topicPath: TopicPath) extends EventsourcedProcessor with ActorLogging {

  implicit def executionContext = context.dispatcher
  val system = context.system

  val timeout = akka.util.Timeout(Settings(context.system).Timeout)
  val retentionTime = Settings(system).Topic.RetentionTime

  override def preStart() = {
    system.scheduler.schedule(retentionTime, retentionTime, self, TopicContentProtocol.PurgeTopicContent)
    super.preStart()
  }

  val receiveRecover: Receive = { 
    case _  => log.debug("no recovery write only model")
  }

  val receiveCommand: Receive = {
    case Publish(message,replyTo)  => publishMessage(message, replyTo)
    case DeleteContent             => deleteTopicContent()
    case PurgeTopicContent         => purgeOldContent()
    case PurgeFrom(id)             => deleteMessages(id, true)
    case FwProcessorId(replyTo)    => replyTo ! TopicContentProtocol.ProcessorId(processorId)
  }

  def purgeOldContent() {
    val timeLimit = System.currentTimeMillis - retentionTime.toMillis
    context.actorOf(TopicPurgerHelper.props(processorId, timeLimit), "purger-helper")                  
  } 

  def deleteTopicContent() {
    if (lastSequenceNr != 0){
      // erase all data from storage
      deleteMessages(lastSequenceNr)
      deleteSnapshots(SnapshotSelectionCriteria.Latest)
    }
    context stop self
  }

  def publishMessage(message: String, replyTo : ActorRef) = {
    val event = Message(lastSequenceNr + 1, topicPath, message)
    persist(event) { evt => 
      context.parent ! TopicContentProtocol.Saved(replyTo)
    }
  }
}

object TopicContentProtocol {
  case class Publish(message: String, replyTo : ActorRef)
  case class Saved(replyTo : ActorRef)
  case class FwProcessorId(subscriber: ActorRef) 
  case class ProcessorId(processorId: String) 
  case class PurgeFrom(id : Long)
  case object DeleteContent
  case object PurgeTopicContent
}

object TopicContent {  
  def props(topicPath: TopicPath) = Props(classOf[TopicContent], topicPath).withDispatcher("topics-dispatcher")
}