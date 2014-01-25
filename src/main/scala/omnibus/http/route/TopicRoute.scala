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

import omnibus.http.JsonSupport._
import omnibus.domain._
import omnibus.domain.subscriber._
import omnibus.domain.topic._
import omnibus.configuration._
import omnibus.service._
import omnibus.service.OmnibusServiceProtocol._

class TopicRoute(omnibusService: ActorRef) (implicit context: ActorContext) extends Directives {

  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(context.system).Timeout.Ask)

  val log: Logger = LoggerFactory.getLogger("omnibus.route.topic")

  lazy val HALType = register(
    MediaType.custom(
      mainType = "application",
      subType = "hal+json",
      compressible = false,
      binary = false
     ))

  val route =
    path("topics" / Rest) { topic =>  
      get {
        respondWithMediaType(HALType) {
          complete {
            if (topic.isEmpty) (omnibusService ? OmnibusServiceProtocol.AllRoots).mapTo[List[TopicView]]
            else (omnibusService ? OmnibusServiceProtocol.ViewTopic(topic)).mapTo[TopicView]
          }
        }
      } ~
      post { ctx =>
        val futureExist = (omnibusService ? OmnibusServiceProtocol.CheckTopic(topic)).mapTo[Boolean]
        futureExist.onComplete {
          case Success(exists) => {
            if(exists) complete(StatusCodes.Accepted, Location(ctx.request.uri):: Nil, s"Topic $topic already exist \n")
            else {                      
              omnibusService ! OmnibusServiceProtocol.CreateTopic(topic)
              ctx.complete (StatusCodes.Created, Location(ctx.request.uri):: Nil, s"Topic $topic created \n") 
            }
          }
          case Failure(ex) => ctx.complete(ex)
        } 
      } ~
      entity(as[String]) { message =>
        put { ctx =>
          val futurePub = (omnibusService ? OmnibusServiceProtocol.PublishToTopic(topic, message)).mapTo[Boolean]
          futurePub.onComplete {
            case Success(ok) => ctx.complete(StatusCodes.Accepted, s"Message published to topic $topic\n")
            case Failure(ex) => ctx.complete(ex)
          } 
        }
      }
    } ~ 
    pathPrefix("stream") {
      path("topics" / Rest) { topic =>
        validate(!topic.isEmpty, "topic name cannot be empty \n") {    
          parameters('react.as[String] ? "simple", 'since.as[Long]?, 'to.as[Long]?, 'sub.as[String] ? "classic").as(ReactiveCmd) { reactiveCmd =>
            get { ctx =>
              val future = (omnibusService ? OmnibusServiceProtocol.SubToTopic(topic, ctx.responder, reactiveCmd, true)).mapTo[Boolean]
              future.onComplete {
                case Success(result) => log.debug("Alles klar, let's stream")
                case Failure(ex)     => ctx.complete(ex)
              }
            }
          }
        }
      }
    } ~
    path("leaves") {
      get { ctx =>
        omnibusService ! OmnibusServiceProtocol.AllLeaves(ctx.responder)
        context.system.scheduler.scheduleOnce(10.seconds){ctx.complete("Connection closes")}
      }
    }

}