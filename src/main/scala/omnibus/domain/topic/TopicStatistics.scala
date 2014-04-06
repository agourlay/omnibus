package omnibus.domain.topic

import akka.actor._

import scala.language.postfixOps
import scala.concurrent.duration._

import nl.grons.metrics.scala.Counter

import omnibus.core.Instrumented
import omnibus.domain.topic.TopicStatProtocol._

class TopicStatistics(val topicRef : ActorRef) extends Actor with ActorLogging with Instrumented {

  implicit val system = context.system
  implicit def executionContext = context.dispatcher

  lazy val prettyPath = TopicPath.prettyStr(topicRef)

  var messageReceived = metrics.meter(s"$prettyPath-meter-messageReceived")
  val subscribersNumber = metrics.counter(s"$prettyPath-subscribersNumber")
  val subTopicsNumber = metrics.counter(s"$prettyPath-subTopicsNumber")

  log.debug(s"Creating new TopicStats for $prettyPath")

  def receive = {
    case MessageReceived   => messageReceived.mark() 
    case SubscriberAdded   => subscribersNumber += 1
    case SubscriberRemoved => subscribersNumber -= 1
    case SubTopicAdded     => subTopicsNumber += 1
    case SubTopicRemoved   => subTopicsNumber -= 1
    case LiveStats         => sender ! liveStats()
  }

  def liveStats() = {
    val currentStat = TopicStatisticValue(prettyPath, throughput, subscribersNumber.count, subTopicsNumber.count)
    currentStat
  }

  def throughput = Math.round(messageReceived.oneMinuteRate*100.0)/100.0
}

object TopicStatProtocol {
  case object MessageReceived
  case object SubscriberAdded
  case object SubscriberRemoved
  case object SubTopicAdded
  case object SubTopicRemoved
  case object LiveStats
  case class TopicStats(stats : List[TopicStatisticValue])
}

object TopicStatistics {
  def props(ref : ActorRef) = Props(classOf[TopicStatistics], ref).withDispatcher("statistics-dispatcher") 
}