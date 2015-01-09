package omnibus.api.route

import akka.actor.{ ActorRef, ActorContext }

import spray.routing._
import spray.routing.authentication._

import omnibus.api.endpoint.RestRequest._
import omnibus.service.classic.{ AllSubscribers, DeleteSubscriber, DeleteTopic, Subscriber }
import omnibus.configuration.Security
import omnibus.domain.topic.TopicPath

class AdminRoute(topicRepo: ActorRef, subRepo: ActorRef)(implicit context: ActorContext) extends Directives {

  implicit def executionContext = context.dispatcher

  val route =
    pathPrefix("admin") {
      authenticate(BasicAuth(Security.adminPassAuthenticator _, realm = "secure site")) { userName ⇒
        pathPrefix("topics" / Rest) { topic ⇒
          validate(!topic.isEmpty, "topic name cannot be empty \n") {
            delete { ctx ⇒
              perRequest(ctx) {
                DeleteTopic.props(TopicPath(topic), topicRepo)
              }
            }
          }
        } ~
          path("subscribers") {
            get { ctx ⇒
              perRequest(ctx) {
                AllSubscribers.props(subRepo)
              }
            }
          } ~
          path("subscribers" / Rest) { sub ⇒
            validate(!sub.isEmpty, "sub id cannot be empty \n") {
              get { ctx ⇒
                perRequest(ctx) {
                  Subscriber.props(sub, subRepo)
                }
              } ~
                delete { ctx ⇒
                  perRequest(ctx) {
                    DeleteSubscriber.props(sub, subRepo)
                  }
                }
            }
          }
      }
    }
}