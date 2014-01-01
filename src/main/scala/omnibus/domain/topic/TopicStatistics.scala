package omnibus.domain.topic

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection.mutable.ListBuffer

import omnibus.domain._
import omnibus.domain.topic.TopicStatProtocol._

class TopicStatistics(val topicName: String, val topicRef : ActorRef) extends Actor with ActorLogging {

  implicit val system = context.system
  implicit def executionContext = context.dispatcher

  // TODO setup through config
  val intervalMeasure : FiniteDuration = 5 minute

  var messageReceived : Long = 0
  var subscribersNumber: Long = 0
  var subTopicsNumber : Long = 0

  var lastMeasureMillis = System.currentTimeMillis

  // TODO will store this somewhere somehow
  var statHistory = ListBuffer.empty[TopicStatisticState]

  override def preStart() = {
    system.scheduler.schedule(intervalMeasure, intervalMeasure, self, TopicStatProtocol.StoringTick)
    log.info(s"Creating new TopicStats for $topicName")
  }

  def receive = {
    case message: Message    => messageReceived = messageReceived + 1
    case SubscriberAdded     => subscribersNumber = subscribersNumber + 1
    case SubscriberRemoved   => subscribersNumber = subscribersNumber - 1
    case SubTopicAdded       => subTopicsNumber = subTopicsNumber + 1
    case SubTopicRemoved     => subTopicsNumber = subTopicsNumber - 1
    case StoringTick         => storeStats()
    case PastStats           => sender ! statHistory.toList
    case LiveStats           => sender ! liveStats()
  }

   def liveStats() : TopicStatisticState = {
    val intervalInSec = (System.currentTimeMillis - lastMeasureMillis)  / 1000
    val throughputPerSec = messageReceived / intervalInSec
    val currentStat = TopicStatisticState(topicName, throughputPerSec, subscribersNumber, subTopicsNumber)
    currentStat
  }

  def storeStats() = {
    val throughputPerSec = messageReceived / intervalMeasure.toSeconds
    val currentStat = TopicStatisticState(topicName, throughputPerSec, subscribersNumber, subTopicsNumber)
    currentStat +=: statHistory
    messageReceived = 0
  }
}

object TopicStatProtocol {
  case object StoringTick
  case object SubscriberAdded
  case object SubscriberRemoved
  case object SubTopicAdded
  case object SubTopicRemoved
  case object PastStats
  case object LiveStats
}