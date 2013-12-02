package omnibus.service

import akka.pattern._
import akka.actor._

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.httpx.encoding._
import spray.routing._
import spray.can.Http
import spray.can.server.Stats

import scala.concurrent.duration._
import scala.concurrent.duration._
import scala.concurrent.Future

import DefaultJsonProtocol._
import reflect.ClassTag

import omnibus.domain.JsonSupport._
import omnibus.service.TopicServiceProtocol._
import omnibus.service.SubscriptionServiceProtocol._


class OmnibusRest(topicService: ActorRef, subscriptionService: ActorRef) extends HttpServiceActor with ActorLogging{
  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(10 seconds)

  def receive = runRoute(topicsRoute ~ statsRoute)
      
  def topicsRoute = 
    path("topics" / Rest) { topic =>
      post {
        complete {
          (topicService ? TopicServiceProtocol.CreateTopic(topic)).mapTo[String]
          }
        } ~
        put {
          entity(as[String]) { message =>
            complete {
              (topicService ? TopicServiceProtocol.PublishToTopic(topic, message)).mapTo[String]
            }
          }
        } ~
        delete {
          complete {
            (topicService ? TopicServiceProtocol.DeleteTopic(topic)).mapTo[String]
          }
        } ~
        get { ctx =>
          subscriptionService ! SubscriptionServiceProtocol.HttpSubscribeToTopic(topic, ctx.responder)
        } ~
        head {
          complete {
            (topicService ? TopicServiceProtocol.CheckTopic(topic)).mapTo[String]
          }
        }
      }

  def statsRoute = 
    path("stats") {
      complete {
        (context.actorSelection("/user/IO-HTTP/listener-0") ? Http.GetStats).mapTo[Stats]
      }
    }
}