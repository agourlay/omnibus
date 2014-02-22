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
import spray.httpx.marshalling._
import HttpHeaders._
import MediaTypes._

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util._

import DefaultJsonProtocol._
import reflect.ClassTag

import omnibus.http.JsonSupport._
import omnibus.http.route._
import omnibus.domain._
import omnibus.domain.topic._
import omnibus.domain.subscriber._
import omnibus.configuration._

class HttpEndpoint(httpStatService : ActorRef, topicRepo : ActorRef, subRepo : ActorRef) extends HttpServiceActor with ActorLogging {

  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(context.system).Timeout.Ask)

  implicit def omnibusExceptionHandler(implicit log: LoggingContext) = ExceptionHandler {
  	case e : TopicNotFoundException  =>
    	requestUri { uri =>
        log.warning("Request to {} could not be handled normally -> topic does not exist", uri)
    	  complete(StatusCodes.NotFound, s"Topic ${e.topicName} not found : please retry later or check topic name correctness\n")
    	}

    case e : TopicAlreadyExistsException =>
      requestUri { uri => 
        log.warning("Request to {} could not be handled normally -> topic {} already exists", uri, e.topicName)
        complete(StatusCodes.Accepted, Location(uri):: Nil, s"Topic ${e.topicName} already exist \n")
      }    

    case e : SubscriberNotFoundException  =>
      requestUri { uri =>
        log.warning("Request to {} could not be handled normally -> subscriber does not exist", uri)
        complete(StatusCodes.NotFound, s"Subscriber ${e.subId} not found : please retry later or check subscriber id correctness\n")
      }

    case e : AskTimeoutException  =>
      requestUri { uri =>
        log.error("Request to {} could not be handled normally -> AskTimeoutException", uri)
        log.error("AskTimeoutException : {} ", e)
        complete(StatusCodes.InternalServerError, "Something is taking longer than expected, retry later \n")
      }

    case e : CircuitBreakerOpenException  =>
      requestUri { uri =>
        log.error("Request to {} could not be handled normally -> CircuitBreakerOpenException", uri)
        log.error("CircuitBreakerOpenException : {} ", e)
        complete(StatusCodes.InternalServerError, "The system is currently under load and cannot process your request, retry later \n")
      } 

    case e : IllegalArgumentException  => 
      requestUri { uri =>
        log.error("Request to {} could not be handled normally -> IllegalArgumentException", uri)
        log.error("IllegalArgumentException : {} ", e)
        complete(StatusCodes.InternalServerError, e.getMessage)
      }  

  	case e : Exception  =>
    	requestUri { uri =>
        log.error("Request to {} could not be handled normally -> unknown exception", uri)
        log.error("unknown exception : {} ", e)
    	  complete(StatusCodes.InternalServerError, "An unexpected error occured \n")
    	}
  }

  val routes =
    new TopicRoute(subRepo, topicRepo).route ~       // '/topics'
    new StatsRoute(httpStatService, topicRepo).route ~      // '/stats'
    new AdminRoute(topicRepo, subRepo).route ~              // '/admin/topics'
    new AdminUIRoute().route                                // '/ '

  def receive = runRoute(routes)

}

object HttpEndpoint {
	def props(httpStatService : ActorRef, topicRepo : ActorRef, subRepo : ActorRef)
    = Props(classOf[HttpEndpoint], httpStatService, topicRepo, subRepo)
}