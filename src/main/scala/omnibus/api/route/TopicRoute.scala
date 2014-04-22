package omnibus.api.route

import akka.actor._

import spray.routing._

import scala.concurrent.duration._

import omnibus.domain.subscriber.ReactiveCmd
import omnibus.domain.topic._
import omnibus.api.request._

class TopicRoute(subRepo: ActorRef, topicRepo : ActorRef) (implicit context: ActorContext) extends Directives {

  implicit def executionContext = context.dispatcher

  val route =
    path("topics") {
      get { ctx =>
        context.actorOf(TopicRoots.props(ctx, topicRepo))
      }
    } ~
    path("topics" / Rest) { topic =>
      validate(!topic.isEmpty, "topic name cannot be empty \n") {  
        val topicPath = TopicPath(topic)
        get { ctx =>
          context.actorOf(ViewTopic.props(topicPath, ctx, topicRepo)) 
        } ~
        post { ctx => 
          context.actorOf(CreateTopic.props(topicPath, ctx, topicRepo)) 
        } ~
        entity(as[String]) { message =>
          put { ctx => context.actorOf(Publish.props(topicPath, message, ctx, topicRepo)) }
        }
      }  
    } ~ 
    pathPrefix("streams") {
      path("topics" / Rest) { topic =>
        validate(!topic.isEmpty, "topic name cannot be empty \n") {
          parameters('react.as[String] ? "simple", 'since.as[Long]?, 'to.as[Long]?).as(ReactiveCmd) { reactiveCmd =>
            clientIP { ip =>
              get { ctx =>
                context.actorOf(Subscribe.props(TopicPath(topic), reactiveCmd, ip.toOption.get.toString, ctx, subRepo, topicRepo))
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