package omnibus.api.stats

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps

import spray.can.server.Stats
import spray.can.Http

import omnibus.api.stats.HttpStatisticsProtocol._

class HttpStatistics extends Actor with ActorLogging {

  implicit val system = context.system
  implicit def executionContext = context.dispatcher

  var lastKnownState : Option[Stats] = None
  var statScheduler : Cancellable = _
 
  override def preStart() = {
    statScheduler = system.scheduler.schedule(1.second, 1.second){
      context.actorSelection("/user/IO-HTTP/listener-0") ! Http.GetStats
    }
  }

  override def postStop() = {
    statScheduler.cancel()
  } 

  def receive = {
    case stat :Stats  => lastKnownState = Some(stat)
    case LiveStats    => sender ! liveStats()
  }

  def liveStats() =  HttpStats.fromStats(lastKnownState.get)
}

object HttpStatisticsProtocol {
  case object LiveStats
}

object HttpStatistics {
  def props = Props(classOf[HttpStatistics]).withDispatcher("statistics-dispatcher") 
}