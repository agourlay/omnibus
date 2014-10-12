package omnibus.api.route

import akka.actor.{ Actor, ActorRef, Props, ActorContext }

import spray.routing._

import scala.concurrent.duration._

import omnibus.domain.subscriber.ReactiveCmd
import omnibus.domain.topic._
import omnibus.domain.subscriber.SubscriptionDescription
import omnibus.domain.subscriber.SubscriberSupport
import omnibus.domain.subscriber.SubscriberSupport._
import omnibus.api.request._
import omnibus.service.streamed.StreamTopicEvent
import omnibus.api.streaming.sse.{ ServerSentEventSupport, ServerSentEventResponse }
import omnibus.service.streamed.StreamTopicLeaves

class TopicRoute(subRepo: ActorRef, topicRepo: ActorRef)(implicit context: ActorContext) extends Directives {

  implicit def executionContext = context.dispatcher

  val route =
    path("topics") {
      get { ctx ⇒
        context.actorOf(RootTopics.props(ctx, topicRepo))
      }
    } ~
      pathPrefix("topics" / Rest) { topic ⇒
        validate(!topic.isEmpty, "topic name cannot be empty \n") {
          val topicPath = TopicPath(topic)
          get { ctx ⇒
            context.actorOf(ViewTopic.props(topicPath, ctx, topicRepo))
          } ~
            post { ctx ⇒
              context.actorOf(CreateTopic.props(topicPath, ctx, topicRepo))
            } ~
            entity(as[String]) { message ⇒
              put { ctx ⇒ context.actorOf(Publish.props(topicPath, message, ctx, topicRepo)) }
            }
        }
      } ~
      pathPrefix("streams") {
        pathPrefix("topics" / Rest) { topic ⇒
          validate(!topic.isEmpty, "topic name cannot be empty \n") {
            ServerSentEventSupport.lastEventId { lei ⇒
              parameters('react.as[String] ? "simple", 'since.as[Long]?, 'to.as[Long]?).as(ReactiveCmd) { reactiveCmd ⇒
                val cmd = if (lei.isDefined) reactiveCmd.copy(since = lei.map(_.toLong)) else reactiveCmd
                clientIP { ip ⇒
                  get { ctx ⇒
                    val sseHolder = context.actorOf(ServerSentEventResponse.props(ctx))
                    val sd = SubscriptionDescription(TopicPath(topic), cmd, ip.toOption.get.toString, SubscriberSupport.SSE)
                    context.actorOf(StreamTopicEvent.props(sseHolder, sd, subRepo, topicRepo))
                  }
                }
              }
            }
          }
        }
      } ~
      path("leaves") {
        get { ctx ⇒
          val sseHolder = context.actorOf(ServerSentEventResponse.props(ctx))
          context.actorOf(StreamTopicLeaves.props(sseHolder, topicRepo))
        }
      }
}