package omnibus.http.route

import akka.actor._

import spray.json._
import spray.routing._
import spray.http._

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

import omnibus.domain.subscriber._
import omnibus.domain.topic._
import omnibus.repository._
import omnibus.http.request._

class TopicRoute(subRepo: ActorRef, topicRepo : ActorRef) (implicit context: ActorContext) extends Directives {

  implicit def executionContext = context.dispatcher

  val log: Logger = LoggerFactory.getLogger("omnibus.route.topic")

  val route =
    path("topics") {
      get { ctx =>
        context.actorOf(TopicRootsRequest.props(ctx, topicRepo))
      }
    } ~
    path("topics" / Rest) { topic =>
      validate(!topic.isEmpty, "topic name cannot be empty \n") {  
        val topicPath = TopicPath(topic)
        get { ctx =>
          context.actorOf(TopicViewRequest.props(topicPath, ctx, topicRepo)) 
        } ~
        post { ctx => 
          context.actorOf(CreateTopicRequest.props(topicPath, ctx, topicRepo)) 
        } ~
        entity(as[String]) { message =>
          put { ctx => context.actorOf(PublishRequest.props(topicPath, message, ctx, topicRepo)) }
        }
      }  
    } ~ 
    pathPrefix("streams") {
      path("topics" / Rest) { topic =>
        validate(!topic.isEmpty, "topic name cannot be empty \n") {
          parameters('react.as[String] ? "simple", 'since.as[Long]?, 'to.as[Long]?).as(ReactiveCmd) { reactiveCmd =>
            clientIP { ip =>
              get { ctx =>
                context.actorOf(SubscribeRequest.props(TopicPath(topic), reactiveCmd, ip.toOption.get.toString, ctx, subRepo, topicRepo))
              }
            }
          }
        }
      }
    } ~
    path("leaves") {
      get { ctx =>
        topicRepo ! TopicRepositoryProtocol.AllLeaves(ctx.responder)
        context.system.scheduler.scheduleOnce(10.seconds){ctx.complete("Connection closed")}
      }
    }
}