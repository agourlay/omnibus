package omnibus.http.route

import akka.pattern._
import akka.actor._

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.httpx.encoding._
import spray.routing._
import spray.can.Http
import spray.can.server.Stats

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
import omnibus.domain._
import omnibus.service._
import omnibus.service.OmnibusServiceProtocol._

class AdminRoute(omnibusService: ActorRef) (implicit context: ActorContext) extends Directives {

  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(5 seconds)

  val log: Logger = LoggerFactory.getLogger("omnibus.route.stat")

  // TODO setup admin authentication 
  val route =
    pathPrefix("admin") {
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