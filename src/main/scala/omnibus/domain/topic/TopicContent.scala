package omnibus.domain.topic

import akka.actor._
import akka.pattern.CircuitBreaker
import akka.persistence._

import scala.language.postfixOps

import omnibus.configuration._
import omnibus.domain._
import omnibus.domain.message._
import omnibus.domain.topic.TopicContentProtocol._
import omnibus.domain.subscriber.ReactiveCmd
import omnibus.domain.subscriber.ReactiveMode

class TopicContent(val topicPath: TopicPath) extends EventsourcedProcessor with ActorLogging {

  implicit val system = context.system
  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(context.system).Timeout.Ask)

  // TODO
  def numEvents = 0L

  val retentionTime = Settings(system).Topic.RetentionTime

  val cb = new CircuitBreaker(system.scheduler,
      maxFailures = 5,
      callTimeout = timeout.duration,
      resetTimeout = timeout.duration * 10).onOpen(log.warning("CircuitBreaker is now open"))
                                           .onClose(log.warning("CircuitBreaker is now closed"))
                                           .onHalfOpen(log.warning("CircuitBreaker is now half-open"))

  override def preStart() = {
    system.scheduler.schedule(retentionTime, retentionTime, self, TopicContentProtocol.PurgeTopicContent)
    super.preStart()
  }

  val receiveRecover: Receive = { 
    case _  => log.debug("no recovery write only model")
  }

  val receiveCommand: Receive = {
    case Publish(message,replyTo)  => cb.withSyncCircuitBreaker(publishMessage(message, replyTo))
    case DeleteContent             => deleteTopicContent()
    case PurgeTopicContent         => purgeOldContent()
    case FwProcessorId(replyTo)    => replyTo ! TopicContentProtocol.ProcessorId(processorId)
  }

  // TODO
  def purgeOldContent() {
    val timeLimit = System.currentTimeMillis - retentionTime.toMillis                  
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
    // persist in topic state
    val event = Message(lastSequenceNr + 1, topicPath, message)
    persist(event) { evt => context.parent ! TopicContentProtocol.Saved(evt, replyTo) }
  }
}

object TopicContentProtocol {
  case class Publish(message: String, replyTo : ActorRef)
  case class Saved(message: Message, replyTo : ActorRef)
  case class FwProcessorId(subscriber: ActorRef) 
  case class ProcessorId(processorId: String) 
  case object DeleteContent
  case object PurgeTopicContent
}

object TopicContent {  
  def props(topicPath: TopicPath) = Props(classOf[TopicContent], topicPath).withDispatcher("topics-dispatcher")
}