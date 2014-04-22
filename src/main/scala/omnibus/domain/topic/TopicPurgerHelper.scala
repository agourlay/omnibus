package omnibus.domain.topic

import akka.actor._
import akka.persistence._

import scala.concurrent.duration._

import omnibus.domain.message.Message
import omnibus.domain.topic.TopicContentProtocol._
import omnibus.domain.topic.TopicPurgerHelperProtocol._

class TopicPurgerHelper(val topicId : String, val timeLimit : Long) extends View with ActorLogging{

	implicit val system = context.system
    implicit def executionContext = context.dispatcher

	override def processorId = topicId

	var lastMatchingId : Option[Long] = None

	val replyDelay = 30 seconds

	system.scheduler.schedule(replyDelay, replyDelay, self, Reply)

	def receive = {
    	case Persistent(payload, _) => self ! payload
  		case msg : Message          => if (msg.timestamp < timeLimit) lastMatchingId = Some(msg.id)
  		case Reply                  => replyToParent()
  	}

  	def replyToParent() = {
		lastMatchingId match {
			case Some(id) => context.parent ! PurgeFrom(id)
			case None     => log.info("Nothing to purge yet")
		}
		self ! PoisonPill
  	}
}

object TopicPurgerHelper {  
  def props(topic: String, timeLimit: Long) = Props(classOf[TopicPurgerHelper], topic, timeLimit).withDispatcher("topics-dispatcher")
}

object TopicPurgerHelperProtocol {
	case object Reply
}