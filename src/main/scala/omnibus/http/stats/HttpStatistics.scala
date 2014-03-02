package omnibus.http.stats

import akka.actor._
import akka.persistence._
import akka.pattern._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection.mutable.ListBuffer

import spray.can.server.Stats
import spray.can.Http

import omnibus.configuration._
import omnibus.http.stats.HttpStatisticsProtocol._

class HttpStatistics extends EventsourcedProcessor with ActorLogging {

  implicit val system = context.system
  implicit def executionContext = context.dispatcher
  
  val timeout = akka.util.Timeout(Settings(context.system).Timeout.Ask)
  val storageInterval = Settings(system).Statistics.StorageInterval
  val retentionTime = Settings(system).Statistics.RetentionTime
  val sampling = Settings(system).Statistics.Sampling

  var state = HttpStatisticsState()
  def updateState(msg: HttpStats): Unit = {state = state.update(msg)}
  def numEvents = state.size

  var lastKnownState : Option[Stats] = None

  val cb = new CircuitBreaker(system.scheduler,
    maxFailures = 5,
    callTimeout = timeout.duration,
    resetTimeout = timeout.duration * 10).onOpen(log.warning("CircuitBreaker is now open"))
                                         .onClose(log.warning("CircuitBreaker is now closed"))
                                         .onHalfOpen(log.warning("CircuitBreaker is now half-open"))

  override def preStart() = {
    system.scheduler.schedule(storageInterval, storageInterval, self, HttpStatisticsProtocol.StoringTick)
    system.scheduler.schedule(retentionTime, retentionTime, self, HttpStatisticsProtocol.PurgeOldData)
    system.scheduler.schedule(sampling, sampling){
       context.actorSelection("/user/IO-HTTP/listener-0") ! Http.GetStats
    }
    log.debug(s"Creating system HttpStatistics holder")
    super.preStart()
  }

  val receiveRecover: Receive = {
    case s : HttpStats                                   => updateState(s)
    case SnapshotOffer(_, snapshot: HttpStatisticsState) => state = snapshot
  }

  val receiveCommand : Receive = {
    case stat :Stats         => lastKnownState = Some(stat)
    case StoringTick         => storeStats()
    case PastStats           => sender ! cb.withSyncCircuitBreaker(state.events.reverse)
    case LiveStats           => sender ! liveStats()
    case PurgeOldData        => purgeOldData()
  }

   def liveStats() : HttpStats = {
    val seqNumber = lastSequenceNr + 1
    val currentStat = HttpStats.fromStats(seqNumber, lastKnownState.get)
    currentStat
  }

  def storeStats() = {
    persist(liveStats()) { s => updateState(s) }
  }

  def purgeOldData() {
    val timeLimit = System.currentTimeMillis - retentionTime.toMillis
    val limitStat = state.events.find(_.timestamp < timeLimit)
    limitStat match {
      case Some(stat) => {
        deleteMessages(stat.seqNumber, true)
        state = HttpStatisticsState(state.events.filterNot(_.timestamp < timeLimit))
      }  
      case None       =>  log.debug(s"Nothing to purge yet in HttpStatistics")
    }                   
  }
}

object HttpStatisticsProtocol {
  case object StoringTick
  case object PastStats
  case object LiveStats
  case object PurgeOldData
}

object HttpStatistics {
  def props = Props(classOf[HttpStatistics]).withDispatcher("statistics-dispatcher") 
}