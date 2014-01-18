package omnibus.http.route

import akka.pattern._
import akka.actor._

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.httpx.encoding._
import spray.routing._
import spray.can.Http
import spray.routing.authentication._

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util._

import DefaultJsonProtocol._
import reflect.ClassTag

import omnibus.http.JsonSupport._
import omnibus.http.streaming._
import omnibus.configuration._
import omnibus.domain._
import omnibus.service._
import omnibus.service.OmnibusServiceProtocol._

class AdminRoute(omnibusService: ActorRef) (implicit context: ActorContext) extends Directives {

  implicit def executionContext = context.dispatcher
  implicit def system = context.system
  implicit val timeout = akka.util.Timeout(Settings(system).Timeout.Ask)

  val log: Logger = LoggerFactory.getLogger("omnibus.route.admin")

  val route =
    pathPrefix("admin") {
      authenticate(BasicAuth(Security.adminPassAuthenticator _, realm = "secure site")) { userName =>
        path("topics" / Rest) { topic =>
          validate(!topic.isEmpty, "topic name cannot be empty \n") {
            delete {
              complete {
                (omnibusService ? OmnibusServiceProtocol.DeleteTopic(topic)).mapTo[String]
              }
            }
          }
        } ~ 
        path("leaves") {
          get { ctx =>
            omnibusService ! OmnibusServiceProtocol.AllLeaves(ctx.responder)
          }
        }
      }
    }  
}