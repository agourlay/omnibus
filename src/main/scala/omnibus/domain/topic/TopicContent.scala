package omnibus.domain.topic

import akka.actor._
import akka.pattern._
import akka.persistence._

import scala.concurrent.duration._
import scala.concurrent._
import scala.concurrent.Promise._
import scala.language.postfixOps
import scala.collection.mutable.ListBuffer

import omnibus.configuration._
import omnibus.domain._
import omnibus.domain.PropagationDirection._
import omnibus.domain.topic.TopicProtocol._
import omnibus.domain.topic.TopicContentProtocol._
import omnibus.domain.subscriber._
import omnibus.domain.subscriber.ReactiveCmd
import omnibus.domain.subscriber.ReactiveMode

class TopicContent(val topicPath: TopicPath) extends EventsourcedProcessor with ActorLogging {

  implicit val system = context.system
  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(context.system).Timeout.Ask)

  var state = TopicState()
  def updateState(msg: MessageTopic): Unit = {state = state.update(msg)}
  def numEvents = state.size

  val retentionTime = Settings(system).Topic.RetentionTime

  val cb = new CircuitBreaker(context.system.scheduler,
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
    case m @ MessageTopic(_, msg)               => updateState(m);
    case SnapshotOffer(_, snapshot: TopicState) => state = snapshot
  }

  val receiveCommand: Receive = {
    case Publish(message,replyTo)  => cb.withSyncCircuitBreaker(publishMessage(message, replyTo))
    case DeleteContent             => deleteTopicContent()
    case PurgeTopicContent         => purgeOldContent()
    case ServeReactive(sub, cmd)   => reactiveCmd(sub, cmd)
  }

  def purgeOldContent() {
    val timeLimit = System.currentTimeMillis - retentionTime.toMillis
    val limitEvt = state.events.find(_.msg.timestamp < timeLimit)
    limitEvt match {
      case None      =>  log.debug(s"Nothing to purge yet in topic")
      case Some(evt) =>  {
        deleteMessages(evt.seqNumber, true)
        state = TopicState(state.events.filterNot(_.msg.timestamp < timeLimit))
      }
    }                   
  } 

  def deleteTopicContent() {
    if (!state.events.isEmpty){
      val lastIdSeen = state.events.head.seqNumber
      // erase all data from storage
      deleteMessages(lastIdSeen)
      deleteSnapshots(SnapshotSelectionCriteria.Latest)
      state = TopicState()
    }
    context stop self
  }

  def reactiveCmd(refSub: ActorRef, cmd : ReactiveCmd) = {
    cmd.react match {
      case ReactiveMode.REPLAY     => serveMessagesReplay(refSub)
      case ReactiveMode.LAST       => serveLastMessage(refSub)
      case ReactiveMode.SINCE_ID   => serveMessagesSinceID(refSub, cmd.since.get)
      case ReactiveMode.SINCE_TS   => serveMessagesSinceTS(refSub, cmd.since.get)
      case ReactiveMode.BETWEEN_ID => serveMessagesBetweenID(refSub, cmd.since.get, cmd.to.get)
      case ReactiveMode.BETWEEN_TS => serveMessagesBetweenTS(refSub, cmd.since.get, cmd.to.get)
      case ReactiveMode.SIMPLE     => log.debug("simple subscription")
    }
  }

  def publishMessage(message: String, replyTo : ActorRef) = {
    // persist in topic state
    val event = Message(lastSequenceNr + 1, topicPath, message)
    persist(MessageTopic(event.id, event)) { evt => 
      updateState(evt)
      context.parent ! TopicContentProtocol.Saved(evt.msg, replyTo)
    }
  }

  def serveMessagesReplay(refSub: ActorRef) = {
    if (state.events.nonEmpty) state.events.reverse.foreach { evt => refSub ! evt.msg }
  }  

  def serveLastMessage(refSub: ActorRef) = {
    if (state.events.nonEmpty) refSub ! state.events.head.msg
  }  

  def serveMessagesSinceID(refSub: ActorRef, eventID: Long) = {
    state.events.reverse.filter(_.msg.id > eventID)
                        .foreach { evt => refSub ! evt.msg }
  }

  def serveMessagesSinceTS(refSub: ActorRef, timestamp: Long) = {
    state.events.reverse.filter(_.msg.timestamp > timestamp)
                        .foreach { evt => refSub ! evt.msg }
  }

  def serveMessagesBetweenID(refSub: ActorRef, startId: Long, endId: Long) = {
    state.events.reverse.filter(evt => evt.msg.id >= startId && evt.msg.id <= endId)
                        .foreach { evt => refSub ! evt.msg }
  }

  def serveMessagesBetweenTS(refSub: ActorRef, startTs: Long, endTs: Long) = {
    state.events.reverse.filter(evt => evt.msg.timestamp >= startTs && evt.msg.timestamp <= endTs)
                        .foreach { evt => refSub ! evt.msg }
  }
}

object TopicContentProtocol {
  case class Publish(message: String, replyTo : ActorRef)
  case class Saved(message: Message, replyTo : ActorRef)
  case class ServeReactive(subscriber: ActorRef, cmd : ReactiveCmd) 
  case object DeleteContent
  case object PurgeTopicContent
  case object MessagePublished
}

object TopicContent {  
  def props(topicPath: TopicPath) = Props(classOf[TopicContent], topicPath).withDispatcher("topics-dispatcher")
}