package omnibus.api.route

import akka.actor.{ Actor, ActorRef, Props, ActorContext }

import spray.routing._

import scala.concurrent.duration._

import omnibus.domain.subscriber.ReactiveCmd
import omnibus.domain.topic._
import omnibus.domain.subscriber.SubscriptionDescription
import omnibus.domain.subscriber.SubscriberSupport
import omnibus.domain.subscriber.SubscriberSupport._
import omnibus.api.endpoint.RestRequest._
import omnibus.service.classic.{ CreateTopic, Publish, ViewTopic, RootTopics }
import omnibus.service.streamed.StreamTopicEvent
import omnibus.api.streaming.sse.ServerSentEventSupport._
import omnibus.api.streaming.sse.ServerSentEventResponse
import omnibus.service.streamed.StreamTopicLeaves

class TopicRoute(subRepo: ActorRef, topicRepo: ActorRef)(implicit context: ActorContext) extends Directives {

  implicit def executionContext = context.dispatcher

  val route =
    path("topics") {
      get { ctx ⇒
        perRequest(ctx) {
          RootTopics.props(topicRepo)
        }
      }
    } ~
      pathPrefix("topics" / Rest) { topic ⇒
        validate(!topic.isEmpty, "topic name cannot be empty \n") {
          val topicPath = TopicPath(topic)
          get { ctx ⇒
            perRequest(ctx) {
              ViewTopic.props(topicPath, topicRepo)
            }
          } ~
            post { ctx ⇒
              perRequest(ctx) {
                CreateTopic.props(topicPath, topicRepo)
              }
            } ~
            entity(as[String]) { message ⇒
              put { ctx ⇒
                perRequest(ctx) {
                  Publish.props(topicPath, message, topicRepo)
                }
              }
            }
        }
      } ~
      pathPrefix("streams") {
        pathPrefix("topics" / Rest) { topic ⇒
          validate(!topic.isEmpty, "topic name cannot be empty \n") {
            lastEventId { lei ⇒
              parameters('react.as[String] ? "simple", 'since.as[Long]?, 'to.as[Long]?).as(ReactiveCmd) { reactiveCmd ⇒
                val cmd = if (lei.isDefined) reactiveCmd.copy(since = lei.map(_.toLong)) else reactiveCmd
                clientIP { ip ⇒
                  get { ctx ⇒
                    val sd = SubscriptionDescription(TopicPath(topic), cmd, ip.toOption.get.toString, SubscriberSupport.SSE)
                    serverSentEvent(ctx) {
                      StreamTopicEvent.props(sd, subRepo, topicRepo)
                    }
                  }
                }
              }
            }
          }
        }
      } ~
      path("leaves") {
        get { ctx ⇒
          serverSentEvent(ctx) {
            StreamTopicLeaves.props(topicRepo)
          }
        }
      }
}