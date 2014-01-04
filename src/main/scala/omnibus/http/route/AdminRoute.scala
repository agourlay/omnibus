package omnibus.http.route

import akka.pattern._
import akka.actor._

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.httpx.encoding._
import spray.routing._
import spray.can.Http
import spray.can.server.Stats
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

  val log: Logger = LoggerFactory.getLogger("omnibus.route.stat")

  val route =
    pathPrefix("admin") {
      authenticate(BasicAuth(adminPassAuthenticator _, realm = "secure site")) { userName =>
        path("topics" / Rest) { topic =>
          validate(!topic.isEmpty, "topic name cannot be empty \n") {
            delete {
              complete {
                (omnibusService ? OmnibusServiceProtocol.DeleteTopic(topic)).mapTo[String]
              }
            }
          }
        }
      }
    }  

  def adminPassAuthenticator(userPass: Option[UserPass]): Future[Option[String]] =
  Future {
    if (userPass.exists(up => up.user == Settings(system).Admin.Name && up.pass == Settings(system).Admin.Password))
      Some(Settings(system).Admin.Name)
    else None
  }
}