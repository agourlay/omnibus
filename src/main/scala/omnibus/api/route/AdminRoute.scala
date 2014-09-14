package omnibus.api.route

import akka.actor._

import spray.routing._
import spray.routing.authentication._

import omnibus.configuration.Security
import omnibus.domain.topic.TopicPath
import omnibus.api.request._

class AdminRoute(topicRepo: ActorRef, subRepo: ActorRef)(implicit context: ActorContext) extends Directives {

  implicit def executionContext = context.dispatcher

  val route =
    pathPrefix("admin") {
      authenticate(BasicAuth(Security.adminPassAuthenticator _, realm = "secure site")) { userName =>
        pathPrefix("topics" / Rest) { topic =>
          validate(!topic.isEmpty, "topic name cannot be empty \n") {
            delete { ctx =>
              context.actorOf(DeleteTopic.props(TopicPath(topic), ctx, topicRepo))
            }
          }
        } ~
          path("subscribers") {
            get { ctx =>
              context.actorOf(AllSubscribers.props(ctx, subRepo))
            }
          } ~
          path("subscribers" / Rest) { sub =>
            validate(!sub.isEmpty, "sub id cannot be empty \n") {
              get { ctx =>
                context.actorOf(Subscriber.props(sub, ctx, subRepo))
              } ~
                delete { ctx =>
                  context.actorOf(DeleteSubscriber.props(sub, ctx, subRepo))
                }
            }
          }
      }
    }
}