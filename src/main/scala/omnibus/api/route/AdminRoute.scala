package omnibus.api.route

import akka.actor._

import spray.routing._
import spray.routing.authentication._

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import omnibus.configuration._
import omnibus.domain.topic._
import omnibus.api.request._

class AdminRoute(topicRepo : ActorRef, subRepo : ActorRef) (implicit context: ActorContext) extends Directives {

  implicit def executionContext = context.dispatcher

  val log: Logger = LoggerFactory.getLogger("omnibus.route.admin")

  val route =
    pathPrefix("admin") {
      authenticate(BasicAuth(Security.adminPassAuthenticator _, realm = "secure site")) { userName =>
        path("topics" / Rest) { topic =>
          validate(!topic.isEmpty, "topic name cannot be empty \n") {
            delete { ctx =>
              context.actorOf(DeleteTopicRequest.props(TopicPath(topic), ctx, topicRepo)) 
            }
          }
        } ~ 
        path("subscribers") {
          get { ctx =>
            context.actorOf(AllSubscribersRequest.props(ctx, subRepo)) 
          }  
        } ~ 
        path("subscribers" / Rest) { sub =>
          validate(!sub.isEmpty, "sub id cannot be empty \n") {
            get { ctx =>
              context.actorOf(SubscriberRequest.props(sub, ctx, subRepo)) 
            } ~ 
            delete { ctx =>
              context.actorOf(DeleteSubscriberRequest.props(sub, ctx, subRepo)) 
            }
          }   
        }  
      }
    }  
}