package omnibus.api.route

import akka.actor._

import spray.routing._

import omnibus.domain.topic.TopicPath
import omnibus.api.stats.StatisticsMode
import omnibus.api.stats.StatisticsMode._
import omnibus.api.request.{HttpLiveStats, ViewTopic, AllMetrics}
import omnibus.api.streaming.{HttpStat, HttpTopicView}

class StatsRoute(httpStatService : ActorRef, topicRepo : ActorRef, metricsRepo : ActorRef)(implicit context: ActorContext) extends Directives {

  val route =
    pathPrefix("stats") {
      parameters('mode.as[StatisticsMode] ? StatisticsMode.LIVE){ mode =>
        path("metrics") {
          get { ctx =>
            context.actorOf(AllMetrics.props(ctx, metricsRepo))
          }
        } ~
        path("system") {
          get { ctx =>
            mode match {
              case StatisticsMode.LIVE      => context.actorOf(HttpLiveStats.props(ctx, httpStatService))
              case StatisticsMode.STREAMING => context.actorOf(HttpStat.props(ctx.responder, httpStatService))
            }
          }
        } ~
        path("topics" / Rest) { topic =>
          validate(!topic.isEmpty, "topic name cannot be empty \n") {
            val topicPath = TopicPath(topic)
            get { ctx =>
              mode match {
                case StatisticsMode.LIVE      => context.actorOf(ViewTopic.props(topicPath, ctx, topicRepo))
                case StatisticsMode.STREAMING => context.actorOf(HttpTopicView.props(topicPath, ctx, topicRepo))
              }    
            }
          }
        }  
      }
    }
  }    