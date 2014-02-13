package omnibus.domain.topic

import akka.actor._
import akka.persistence._
import akka.pattern.CircuitBreaker

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection.mutable.ListBuffer

import omnibus.configuration._
import omnibus.domain._
import omnibus.domain.topic.TopicStatProtocol._

class TopicStatistics(val topicRef : ActorRef) extends EventsourcedProcessor with ActorLogging {

  implicit val system = context.system
  implicit def executionContext = context.dispatcher

  lazy val prettyPath = Topic.prettyPath(topicRef)

  val storageInterval = Settings(system).Statistics.StorageInterval
  val retentionTime = Settings(system).Statistics.RetentionTime
  val resolution = Settings(system).Statistics.Resolution

  var state = TopicStatisticState()
  def updateState(msg: TopicStatisticValue): Unit = {state = state.update(msg)}
  def numEvents = state.size

  var messageReceived : Long = 0
  var subscribersNumber: Long = 0
  var subTopicsNumber : Long = 0

  var lastMeasureMillis = System.currentTimeMillis

  val breaker = new CircuitBreaker(system.scheduler,
      maxFailures = 5,
      callTimeout = 10.seconds,
      resetTimeout = 1.minute)

  override def preStart() = {
    system.scheduler.schedule(storageInterval, storageInterval, self, TopicStatProtocol.StoringTick)
    system.scheduler.schedule(retentionTime, retentionTime, self, TopicStatProtocol.PurgeOldData)
    system.scheduler.schedule(resolution, resolution, self, TopicStatProtocol.ResetCounter)
    log.debug(s"Creating new TopicStats for $prettyPath")
    super.preStart()
  }

  val receiveRecover: Receive = {
    case SnapshotOffer(_, snapshot: TopicStatisticState) => state = snapshot
  }

  val receiveCommand : Receive = {
    case MessageReceived     => messageReceived = messageReceived + 1
    case SubscriberAdded     => subscribersNumber = subscribersNumber + 1
    case SubscriberRemoved   => subscribersNumber = subscribersNumber - 1
    case SubTopicAdded       => subTopicsNumber = subTopicsNumber + 1
    case SubTopicRemoved     => subTopicsNumber = subTopicsNumber - 1
    case StoringTick         => storeStats()
    case PastStats           => sender ! breaker.withSyncCircuitBreaker(state.events.reverse)
    case LiveStats           => sender ! liveStats()
    case PurgeOldData        => purgeOldData()
    case ResetCounter        => resetCounter()
  }

   def liveStats() : TopicStatisticValue = {
    val seqNumber = lastSequenceNr + 1
    val intervalInSec : Double = (System.currentTimeMillis - lastMeasureMillis)  / 1000d
    val throughputPerSec = calculateThroughput(intervalInSec, messageReceived)
    val currentStat = TopicStatisticValue(seqNumber, prettyPath, throughputPerSec, subscribersNumber, subTopicsNumber)
    currentStat
  }

  def storeStats() = {
    persist(liveStats()) { s => updateState(s) }
  }

  def purgeOldData() {
    val limitStat = state.events
                         .find(_.timestamp < (System.currentTimeMillis - retentionTime.toMillis))

    limitStat match {
      case Some(stat) => deleteMessages(stat.seqNumber)
      case None      =>  log.debug(s"Nothing to purge yet in topicStatistic")
    }                   
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
  case object LiveStats
  case object PurgeOldData
}

object TopicStatistics {
  def props(ref : ActorRef) = Props(classOf[TopicStatistics], ref).withDispatcher("statistics-dispatcher") 
}