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
import scala.concurrent.Future

import DefaultJsonProtocol._
import reflect.ClassTag

import omnibus.domain.JsonSupport._
import omnibus.service.OmnibusServiceProtocol._


class OmnibusRest(omnibusService: ActorRef) extends HttpServiceActor with ActorLogging{
  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(10 seconds)

  def receive = runRoute(topicsRoute ~ statsRoute)
      
  def topicsRoute = 
    path("topics" / Rest) { topic =>
      post {
        complete {
          (omnibusService ? OmnibusServiceProtocol.CreateTopic(topic)).mapTo[String]
          }
        } ~
        put {
          entity(as[String]) { message =>
            complete {
              (omnibusService ? OmnibusServiceProtocol.PublishToTopic(topic, message)).mapTo[String]
            }
          }
        } ~
        delete {
          complete {
            (omnibusService ? OmnibusServiceProtocol.DeleteTopic(topic)).mapTo[String]
          }
        } ~
        parameters('mode ? "simple") { mode => 
          get { ctx =>
            omnibusService ! OmnibusServiceProtocol.HttpSubToTopic(topic, ctx.responder, mode)
          }  
        } ~
        head {
          complete {
            (omnibusService ? OmnibusServiceProtocol.CheckTopic(topic)).mapTo[String]
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