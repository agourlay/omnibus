package omnibus.domain.subscriber

import akka.actor.{ Props, ActorLogging }
import akka.persistence._

import omnibus.domain.topic.TopicEvent

class Subscription(val topicId: String, val cmd: ReactiveCmd) extends PersistentView with ActorLogging {

  override def persistenceId = topicId
  override def viewId = topicId + "-view"
  val creationDate = System.currentTimeMillis / 1000L

  override def preStart() = {
    log.debug(s"Creating subscription on $viewId")
    triggerRecoveryWindow()
  }

  def receive = {
    case te: TopicEvent ⇒ if (reactiveFilter(te)) context.parent ! te
  }

  def triggerRecoveryWindow() = {
    cmd.react match {
      case ReactiveMode.SIMPLE     ⇒ self ! Recover() // TODO should be skipped but does not work without
      case ReactiveMode.REPLAY     ⇒ self ! Recover()
      case ReactiveMode.SINCE_ID   ⇒ self ! Recover()
      case ReactiveMode.SINCE_TS   ⇒ self ! Recover()
      case ReactiveMode.BETWEEN_ID ⇒ self ! Recover(toSequenceNr = cmd.to.get)
      case ReactiveMode.BETWEEN_TS ⇒ self ! Recover()
    }
  }

  def reactiveFilter(te: TopicEvent) = {
    cmd.react match {
      case ReactiveMode.REPLAY     ⇒ true
      case ReactiveMode.SIMPLE     ⇒ te.timestamp > creationDate
      case ReactiveMode.SINCE_ID   ⇒ te.id > cmd.since.get
      case ReactiveMode.SINCE_TS   ⇒ te.timestamp > cmd.since.get
      case ReactiveMode.BETWEEN_ID ⇒ te.id >= cmd.since.get && te.id <= cmd.to.get
      case ReactiveMode.BETWEEN_TS ⇒ te.timestamp >= cmd.since.get && te.timestamp <= cmd.to.get
    }
  }
}

object Subscription {
  def props(topicId: String, cmd: ReactiveCmd) = Props(classOf[Subscription], topicId, cmd).withDispatcher("subscribers-dispatcher")
}