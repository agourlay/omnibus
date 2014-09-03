package omnibus.api.exceptions 

import akka.pattern.CircuitBreakerOpenException

import spray.util.LoggingContext
import spray.routing._
import spray.http._
import HttpHeaders._

import omnibus.domain.topic._
import omnibus.domain.subscriber._
import omnibus.metrics.Instrumented

trait RestFailureHandling extends Instrumented {
  this: HttpService =>

  val topicNotFound = metrics.meter("TopicNotFoundException")
  val topicAlreadyExists = metrics.meter("TopicAlreadyExistsException")
  val subscriberNotFound = metrics.meter("SubscriberNotFoundException")
  val requestTimeout = metrics.meter("RequestTimeoutException")
  val illegalArgument = metrics.meter("IllegalArgumentException")
  val circuitBreaker = metrics.meter("CircuitBreakerException")
  val otherException = metrics.meter("OtherException")  

  implicit def omnibusExceptionHandler(implicit log: LoggingContext) = ExceptionHandler {
  	case e : TopicNotFoundException  =>
    	requestUri { uri =>
        topicNotFound.mark()
        log.warning("Request to {} could not be handled normally -> topic does not exist", uri)
    	  complete(StatusCodes.NotFound, s"Topic ${e.topicName} not found : please retry later or check topic name correctness\n")
    	}

    case e : TopicAlreadyExistsException =>
      requestUri { uri =>
        topicAlreadyExists.mark() 
        log.warning("Request to {} could not be handled normally -> topic {} already exists", uri, e.topicName)
        complete(StatusCodes.Accepted, Location(uri):: Nil, s"Topic ${e.topicName} already exist \n")
      }    

    case e : SubscriberNotFoundException  =>
      requestUri { uri =>
        subscriberNotFound.mark()
        log.warning("Request to {} could not be handled normally -> subscriber does not exist", uri)
        complete(StatusCodes.NotFound, s"Subscriber ${e.subId} not found : please retry later or check subscriber id correctness\n")
      }

    case e : RequestTimeoutException  =>
      requestUri { uri =>
        requestTimeout.mark()
        log.error("Request to {} could not be handled normally -> RestRequestTimeout", uri)
        log.error("RestRequestTimeout : {} ", e)
        complete(StatusCodes.InternalServerError, "Something is taking longer than expected, retry later \n")
      }

    case e : CircuitBreakerOpenException  =>
      requestUri { uri =>
        circuitBreaker.mark()
        log.error("Request to {} could not be handled normally -> CircuitBreakerOpenException", uri)
        log.error("CircuitBreakerOpenException : {} ", e)
        complete(StatusCodes.InternalServerError, "Omnibus is currently under high load and cannot process your request, retry later \n")
      } 

    case e : IllegalArgumentException  => 
      requestUri { uri =>
        illegalArgument.mark()
        log.error("Request to {} could not be handled normally -> IllegalArgumentException", uri)
        log.error("IllegalArgumentException : {} ", e)
        complete(StatusCodes.InternalServerError, e.getMessage)
      }  

  	case e : Exception  =>
    	requestUri { uri =>
        otherException.mark()
        log.error("Request to {} could not be handled normally -> unknown exception", uri)
        log.error("unknown exception : {} ", e)
    	  complete(StatusCodes.InternalServerError, "An unexpected error occured \n")
    	}
  }      
}  
