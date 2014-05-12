package omnibus.api.route

import akka.actor._

import spray.routing._

import omnibus.domain.topic.TopicPath
import omnibus.api.endpoint.StatisticsMode
import omnibus.api.endpoint.StatisticsMode._
import omnibus.api.request.{ViewTopic, AllMetrics}
import omnibus.api.streaming.HttpTopicView

class StatsRoute(topicRepo : ActorRef, metricsRepo : ActorRef)(implicit context: ActorContext) extends Directives {

  val route =
    pathPrefix("stats") {
      parameters('mode.as[StatisticsMode] ? StatisticsMode.LIVE){ mode =>
        path("metrics") {
          get { ctx =>
            context.actorOf(AllMetrics.props(ctx, metricsRepo))
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