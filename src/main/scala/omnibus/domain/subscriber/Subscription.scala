package omnibus.domain.subscriber

import akka.actor._
import akka.persistence._

import omnibus.domain.message.Message

class Subscription(val topicId : String, val cmd: ReactiveCmd) extends PersistentView with ActorLogging{

	override def persistenceId = topicId
	override def viewId = topicId + "-view"

	// Automated recovery on start can be disabled by overriding preStart.
	override def preStart() = {
		triggerRecoveryWindow()
	}

	def receive = {
  		case msg : Message => if (reactiveFilter(msg)) context.parent ! msg
  	}

  	def triggerRecoveryWindow() = {
  		cmd.react match {
		    case ReactiveMode.REPLAY     => self ! Recover()
		    case ReactiveMode.SINCE_ID   => self ! Recover(toSequenceNr = cmd.since.get)
		    case ReactiveMode.SINCE_TS   => self ! Recover()
		    case ReactiveMode.BETWEEN_ID => self ! Recover(toSequenceNr = cmd.since.get)
		    case ReactiveMode.BETWEEN_TS => self ! Recover()
	    }
  	}

  	def reactiveFilter(msg: Message) = {
	    cmd.react match {
		    case ReactiveMode.REPLAY     => true
		    case ReactiveMode.SIMPLE     => true
		    case ReactiveMode.SINCE_ID   => msg.id > cmd.since.get
		    case ReactiveMode.SINCE_TS   => msg.timestamp > cmd.since.get
		    case ReactiveMode.BETWEEN_ID => msg.id >= cmd.since.get && msg.id <= cmd.to.get
		    case ReactiveMode.BETWEEN_TS => msg.timestamp >= cmd.since.get && msg.timestamp <= cmd.to.get
	    }
  	}
}

object Subscription {
	def props(topicId : String, cmd : ReactiveCmd) = Props(classOf[Subscription], topicId, cmd).withDispatcher("subscribers-dispatcher")
}	