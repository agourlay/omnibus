package omnibus.domain.topic

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection.mutable.ListBuffer

import omnibus.configuration._
import omnibus.domain._
import omnibus.domain.topic.TopicStatProtocol._

class TopicStatistics(val topicRef : ActorRef) extends Actor with ActorLogging {

  implicit val system = context.system
  implicit def executionContext = context.dispatcher

  lazy val prettyPath = Topic.prettyPath(topicRef)

  val storageInterval = Settings(system).Statistics.StorageInterval
  val retentionTime = Settings(system).Statistics.RetentionTime
  val resolution = Settings(system).Statistics.Resolution

  var messageReceived : Long = 0
  var subscribersNumber: Long = 0
  var subTopicsNumber : Long = 0

  var lastMeasureMillis = System.currentTimeMillis
  var statHistory = ListBuffer.empty[TopicStatisticState]

  override def preStart() = {
    system.scheduler.schedule(storageInterval, storageInterval, self, TopicStatProtocol.StoringTick)
    system.scheduler.schedule(retentionTime, retentionTime, self, TopicStatProtocol.PurgeOldData)
    system.scheduler.schedule(resolution, resolution, self, TopicStatProtocol.ResetCounter)
    log.debug(s"Creating new TopicStats for $prettyPath")
  }

  def receive = {
    case MessageReceived     => messageReceived = messageReceived + 1
    case SubscriberAdded     => subscribersNumber = subscribersNumber + 1
    case SubscriberRemoved   => subscribersNumber = subscribersNumber - 1
    case SubTopicAdded       => subTopicsNumber = subTopicsNumber + 1
    case SubTopicRemoved     => subTopicsNumber = subTopicsNumber - 1
    case StoringTick         => storeStats()
    case PastStats           => sender ! statHistory.toList
    case LiveStats           => sender ! liveStats()
    case PurgeOldData        => purgeOldData()
    case ResetCounter        => resetCounter()
  }

   def liveStats() : TopicStatisticState = {
    val intervalInSec : Double = (System.currentTimeMillis - lastMeasureMillis)  / 1000d
    val throughputPerSec = calculateThroughput(intervalInSec, messageReceived)
    val currentStat = TopicStatisticState(prettyPath, throughputPerSec, subscribersNumber, subTopicsNumber)
    currentStat
  }

  def storeStats() = {
    liveStats() +=: statHistory
  }

  def purgeOldData() {
    statHistory = statHistory.filter(stat => stat.timestamp > (System.currentTimeMillis - retentionTime.toMillis))
  }

  def resetCounter() {
    messageReceived = 0
    lastMeasureMillis = System.currentTimeMillis
  }

  def calculateThroughput(intervalInSec : Double, messageReceived : Long) = {
    val throughputPerSec : Double = messageReceived / intervalInSec
    Math.round(throughputPerSec*100.0)/100.0
  }
}

object TopicStatProtocol {
  case object StoringTick
  case object ResetCounter
  case object MessageReceived
  case object SubscriberAdded
  case object SubscriberRemoved
  case object SubTopicAdded
  case object SubTopicRemoved
  case object PastStats
  case object StreamStats
  case object LiveStats
  case object PurgeOldData
}

object TopicStatistics {
  def props(ref : ActorRef) : Props = Props(classOf[TopicStatistics], ref)
}