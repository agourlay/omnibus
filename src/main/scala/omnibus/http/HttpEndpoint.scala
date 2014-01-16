package omnibus.http

import akka.pattern._
import akka.actor._

import spray.util.LoggingContext
import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.httpx.encoding._
import spray.routing._
import spray.http._
import spray.can.Http
import spray.can.server.Stats

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util._

import DefaultJsonProtocol._
import reflect.ClassTag

import omnibus.http.JsonSupport._
import omnibus.http.route._
import omnibus.domain._
import omnibus.domain.topic.TopicNotFoundException
import omnibus.configuration._
import omnibus.service._
import omnibus.service.OmnibusServiceProtocol._



class HttpEndpoint(omnibusService: ActorRef) extends HttpServiceActor with ActorLogging {


  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(context.system).Timeout.Ask)

  implicit def myExceptionHandler(implicit log: LoggingContext) = ExceptionHandler {
  	case e : TopicNotFoundException  =>
  	requestUri { uri =>
      log.warning("Request to {} could not be handled normally; topic does not exist", uri)
  	  complete(StatusCodes.NotFound, s"No topic ${e.topicName} found; please retry later or check topic name correctness !!!\n")
  	}
  	case e : Exception  =>
  	requestUri { uri =>
      log.warning("Request to {} could not be handled normally; unknown exception", uri)
      log.error("unknown exception : ", e)
  	  complete(StatusCodes.InternalServerError, "An unexpected error occured \n")
  	}
  }

  val routes =
    new TopicRoute(omnibusService).route ~ // '/topics'
    new StatsRoute(omnibusService).route ~ // '/stats'
    new AdminRoute(omnibusService).route ~ // '/admin/topics'
    new AdminUIRoute().route               // '/ '

  def receive = runRoute(routes)

}

object HttpEndpoint {
	def props(omnibusService: ActorRef) : Props = Props(classOf[HttpEndpoint], omnibusService)
}