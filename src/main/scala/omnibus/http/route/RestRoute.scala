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
import scala.concurrent.Future
import scala.util._

import DefaultJsonProtocol._
import reflect.ClassTag

import omnibus.http.JsonSupport._
import omnibus.domain._
import omnibus.domain.subscriber._
import omnibus.service._
import omnibus.service.OmnibusServiceProtocol._

class RestRoute(omnibusService: ActorRef) (implicit context: ActorContext) extends Directives {

  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(5 seconds)

  val log: Logger = LoggerFactory.getLogger("omnibus.route.rest")

  val route =
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
        parameters('mode.as[String] ? "simple", 'since.as[Long]?, 'to.as[Long]?).as(ReactiveCmd) { reactiveCmd =>
          get { ctx =>
            val future = (omnibusService ? OmnibusServiceProtocol.SubToTopic(topic, ctx.responder, reactiveCmd, true)).mapTo[Boolean]
            future.onComplete {
              case Success(result) => log.debug("Alles klar, let's stream")
              case Failure(result) => ctx.complete(s"topic '$topic' not found \n")
            }
          }
        }
      }
    }
}