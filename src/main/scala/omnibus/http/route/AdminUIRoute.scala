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

class AdminUIRoute(implicit context: ActorContext) extends Directives {

  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(5 seconds)

  val log: Logger = LoggerFactory.getLogger("omnibus.route.staticFiles")

  val route = 
    path(""){
        encodeResponse(Gzip){
          getFromResource("frontend/web/index.html")   
      }
    } ~
	  encodeResponse(Gzip){
	    getFromResourceDirectory("frontend/web")
    }    
}