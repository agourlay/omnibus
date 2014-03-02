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

  lazy val prettyPath = TopicPath.prettyStr(topicRef)

  val timeout = akka.util.Timeout(Settings(context.system).Timeout.Ask)
  val storageInterval = Settings(system).Statistics.StorageInterval
  val retentionTime = Settings(system).Statistics.RetentionTime
  val sampling = Settings(system).Statistics.Sampling

  var state = TopicStatisticState()
  def updateState(msg: TopicStatisticValue): Unit = {state = state.update(msg)}
  def numEvents = state.size

  var messageReceived : Long = 0
  var subscribersNumber: Long = 0
  var subTopicsNumber : Long = 0

  var lastMeasureMillis = System.currentTimeMillis

  var toStore = false

  val cb = new CircuitBreaker(system.scheduler,
      maxFailures = 5,
      callTimeout = timeout.duration,
      resetTimeout = timeout.duration * 10).onOpen(log.warning("CircuitBreaker is now open"))
                                           .onClose(log.warning("CircuitBreaker is now closed"))
                                           .onHalfOpen(log.warning("CircuitBreaker is now half-open"))

  override def preStart() = {
    system.scheduler.schedule(storageInterval, storageInterval, self, TopicStatProtocol.StoringTick)
    system.scheduler.schedule(retentionTime, retentionTime, self, TopicStatProtocol.PurgeOldData)
    system.scheduler.schedule(sampling, sampling, self, TopicStatProtocol.ResetCounter)
    log.debug(s"Creating new TopicStats for $prettyPath")
    super.preStart()
  }

  val receiveRecover: Receive = {
    case s : TopicStatisticValue                         => updateState(s)
    case SnapshotOffer(_, snapshot: TopicStatisticState) => state = snapshot
  }

  val receiveCommand : Receive = {
    case MessageReceived     => messageReceived += 1  ; toStore = true
    case SubscriberAdded     => subscribersNumber += 1; toStore = true
    case SubscriberRemoved   => subscribersNumber -= 1; toStore = true
    case SubTopicAdded       => subTopicsNumber += 1  ; toStore = true
    case SubTopicRemoved     => subTopicsNumber -= 1  ; toStore = true

    case StoringTick         => storeStats()
    case PastStats           => sender ! cb.withSyncCircuitBreaker(state.events.reverse)
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
    // avoid persisting data if nothing changed
    if (toStore){
      persist(liveStats()) { s => updateState(s) }
      toStore = false
    }
  }

  def purgeOldData() {
    val timeLimit = System.currentTimeMillis - retentionTime.toMillis
    val limitStat = state.events.find(_.timestamp < timeLimit)

    limitStat match {
      case Some(stat) => {
        deleteMessages(stat.seqNumber, true)
        state = TopicStatisticState(state.events.filterNot(_.timestamp < timeLimit))
      }  
      case None       =>  log.debug(s"Nothing to purge yet in topicStatistic")
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