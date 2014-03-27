package omnibus.domain.subscriber

import akka.actor._
import akka.persistence._

import omnibus.domain.message.Message

class Subscription(val topicId : String, val cmd: ReactiveCmd) extends View with ActorLogging{

	override def processorId = topicId

	val created = System.currentTimeMillis / 1000

	def receive = {
    	case Persistent(payload, _) => self ! payload
  		case msg : Message          => if (reactiveFilter(msg)) context.parent ! msg
  	}

  	def reactiveFilter(msg: Message) = {
	    cmd.react match {
	      case ReactiveMode.REPLAY     => true
	      case ReactiveMode.SIMPLE     => msg.timestamp >= created // FIXME recovery cannot be skipped for views :(
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