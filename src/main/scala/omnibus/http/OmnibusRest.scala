package omnibus.http

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
import scala.util._

import DefaultJsonProtocol._
import reflect.ClassTag

import omnibus.http.JsonSupport._
import omnibus.domain._
import omnibus.service._
import omnibus.service.OmnibusServiceProtocol._


class OmnibusRest(omnibusService: ActorRef) extends HttpServiceActor with ActorLogging{
  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(5 seconds)

  def receive = runRoute(topicsRoute ~ statsRoute)
      
  def topicsRoute = 
    path("topics" / Rest) { topic =>
      validate(!topic.isEmpty, "topic name cannot be empty \n") {
        post {
          entity(as[String]) { message =>
            complete {
              (omnibusService ? OmnibusServiceProtocol.CreateTopic(topic, message)).mapTo[String]
            }
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
        parameters('mode.as[String] ? "simple", 'since.as[Long]?, 'to.as[Long]?).as(ReactiveInput) { reactiveInput => 
          get { ctx =>
            //TODO Use case class extraction on parameters directly on reactiveCmd with require validation 
            val reactiveCmd = ReactiveCmd(reactiveInput)
            val future = (omnibusService ? OmnibusServiceProtocol.SubToTopic(topic, ctx.responder, reactiveCmd, true)).mapTo[Boolean]
            future.onComplete{
              case Success(result) => log.debug("Alles klar, let's stream") 
              case Failure(result) => ctx.complete(s"topic '$topic' not found \n")
            }
          }  
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