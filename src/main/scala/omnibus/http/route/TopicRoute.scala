package omnibus.http.route

import akka.pattern._
import akka.actor._

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.httpx.encoding._
import spray.httpx.marshalling._
import spray.routing._
import spray.can.Http._
import spray.http._
import HttpHeaders._
import MediaTypes._

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util._

import DefaultJsonProtocol._
import reflect.ClassTag

import omnibus.http.CustomMediaType
import omnibus.http.JsonSupport._
import omnibus.domain._
import omnibus.domain.subscriber._
import omnibus.domain.topic._
import omnibus.configuration._
import omnibus.repository._
import omnibus.http.request._

class TopicRoute(subRepo: ActorRef, topicRepo : ActorRef) (implicit context: ActorContext) extends Directives {

  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(context.system).Timeout.Ask)

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