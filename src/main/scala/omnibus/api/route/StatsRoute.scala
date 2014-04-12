package omnibus.api.route

import akka.actor._

import spray.routing._

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import omnibus.api.streaming._
import omnibus.domain.topic._
import omnibus.domain.topic.StatisticsMode._
import omnibus.api.request._

class StatsRoute(httpStatService : ActorRef, topicRepo : ActorRef)(implicit context: ActorContext) extends Directives {

  val log: Logger = LoggerFactory.getLogger("omnibus.route.stat")

  val route =
    pathPrefix("stats") {
      parameters('mode.as[StatisticsMode] ? StatisticsMode.LIVE){ mode =>
        path("system") {
          get { ctx =>
            log.debug(s"Sending system stats with $mode")
            mode match {
              case StatisticsMode.LIVE      => context.actorOf(HttpLiveStatsRequest.props(ctx,httpStatService))
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
                case StatisticsMode.LIVE      => context.actorOf(TopicViewRequest.props(topicPath, ctx, topicRepo))
                case StatisticsMode.STREAMING => context.actorOf(HttpTopicViewStream.props(topicPath, ctx, topicRepo))
              }    
            }
          }
        }  
      }
    }
  }    