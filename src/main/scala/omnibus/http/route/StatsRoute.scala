package omnibus.http.route

import akka.pattern._
import akka.actor._

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.httpx.encoding._
import spray.routing._
import spray.can.Http
import spray.can.server.Stats

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util._

import DefaultJsonProtocol._
import reflect.ClassTag

import omnibus.http.JsonSupport._
import omnibus.http.streaming._
import omnibus.domain.topic._
import omnibus.domain.topic.StatisticsMode._
import omnibus.repository._
import omnibus.configuration._
import omnibus.service._
import omnibus.service.OmnibusServiceProtocol._
import omnibus.http.stats._
import omnibus.http.stats.HttpStatisticsProtocol._

class StatsRoute(omnibusService: ActorRef, httpStatService : ActorRef)
                (implicit context: ActorContext) extends Directives {

  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(context.system).Timeout.Ask)

  val log: Logger = LoggerFactory.getLogger("omnibus.route.stat")

  val route =
    pathPrefix("stats") {
      parameters('mode.as[StatisticsMode] ? StatisticsMode.LIVE){ mode =>
        path("system") {
          get { ctx =>
            log.debug(s"Sending system stats with $mode")
              mode match {
                case StatisticsMode.LIVE      => ctx.complete ((httpStatService ? HttpStatisticsProtocol.LiveStats).mapTo[HttpStats])
                case StatisticsMode.HISTORY   => ctx.complete ((httpStatService ? HttpStatisticsProtocol.PastStats).mapTo[List[HttpStats]])
                case StatisticsMode.STREAMING => context.actorOf(HttpStatStream.props(ctx.responder, httpStatService))
              }
            }
          } ~
          path("topics" / Rest) { topic =>
            validate(!topic.isEmpty, "topic name cannot be empty \n") {
              get { ctx =>
                log.debug(s"Sending stats from topic $topic with $mode")
                mode match {
                  case StatisticsMode.LIVE      => ctx.complete ((omnibusService ? OmnibusServiceProtocol.TopicLiveStat(topic)).mapTo[TopicStatisticValue])
                  case StatisticsMode.HISTORY   => ctx.complete ((omnibusService ? OmnibusServiceProtocol.TopicPastStat(topic)).mapTo[List[TopicStatisticValue]])
                  case StatisticsMode.STREAMING => {
                    val f = (omnibusService ? OmnibusServiceProtocol.LookupTopic(topic)).mapTo[Option[ActorRef]]
                    f.onComplete {
                      case Failure(result) => ctx.complete(s"Something wrong happened... \n")
                      case Success(result) => result match {
                        case Some(ref) => context.actorOf(HttpTopicStatStream.props(ctx.responder, ref))
                        case None      => ctx.complete(s"topic '$topic' not found \n")
                      }
                    }
                  }
                }    
              }
            }
          }  
        }
      }
    }    