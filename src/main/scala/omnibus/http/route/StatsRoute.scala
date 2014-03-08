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
import omnibus.http.request._

class StatsRoute(httpStatService : ActorRef, topicRepo : ActorRef)(implicit context: ActorContext) extends Directives {

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
                case StatisticsMode.LIVE      => context.actorOf(HttpLiveStatsRequest.props(ctx,httpStatService))
                case StatisticsMode.HISTORY   => context.actorOf(HttpPastStatsRequest.props(ctx,httpStatService))
                case StatisticsMode.STREAMING => context.actorOf(HttpStatStream.props(ctx.responder, httpStatService))
              }
            }
          } ~
          path("topics" / Rest) { topic =>
            validate(!topic.isEmpty, "topic name cannot be empty \n") {
              val topicPath = TopicPath(topic)
              val prettyTopic = topicPath.prettyStr()
              get { ctx =>
                log.debug(s"Sending stats from topic $prettyTopic with $mode")
                mode match {
                  case StatisticsMode.LIVE      => context.actorOf(TopicLiveStatsRequest.props(topicPath, ctx, topicRepo))
                  case StatisticsMode.HISTORY   => context.actorOf(TopicPastStatsRequest.props(topicPath, ctx, topicRepo))
                  case StatisticsMode.STREAMING => context.actorOf(HttpTopicStatStream.props(topicPath, ctx, topicRepo))
                }    
              }
            }
          }  
        }
      }
    }    