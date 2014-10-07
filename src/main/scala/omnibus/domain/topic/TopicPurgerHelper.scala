package omnibus.domain.topic

import akka.actor._
import akka.persistence._

import scala.concurrent.duration._

import omnibus.domain.message.Message
import omnibus.domain.topic.TopicContentProtocol._
import omnibus.domain.topic.TopicPurgerHelperProtocol._

class TopicPurgerHelper(val topicId: String, val timeLimit: Long) extends PersistentView with ActorLogging {

  implicit val system = context.system
  implicit def executionContext = context.dispatcher

  override def persistenceId = topicId
  override def viewId = topicId + "-view"

  var lastMatchingId: Option[Long] = None

  val replyDelay = 30 seconds

  val replyScheduler = system.scheduler.schedule(replyDelay, replyDelay, self, Reply)

  def receive = {
    case msg: Message ⇒ if (msg.timestamp < timeLimit) lastMatchingId = Some(msg.id)
    case Reply        ⇒ replyToParent()
  }

  override def postStop() = {
    replyScheduler.cancel()
  }

  def replyToParent() = {
    lastMatchingId match {
      case Some(id) ⇒ context.parent ! PurgeFrom(id)
      case None     ⇒ log.info("Nothing to purge yet")
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