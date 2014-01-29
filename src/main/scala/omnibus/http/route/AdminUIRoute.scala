package omnibus.http.route

import akka.pattern._
import akka.actor._

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.httpx.encoding._
import spray.routing._
import spray.can.Http
import spray.routing.authentication._
import spray.routing.directives.CachingDirectives._

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util._

import DefaultJsonProtocol._
import reflect.ClassTag

import omnibus.configuration._

class AdminUIRoute(implicit context: ActorContext) extends Directives {

  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(context.system).Timeout.Ask)

  val log: Logger = LoggerFactory.getLogger("omnibus.route.adminUI")

  val simpleCache = routeCache(maxCapacity = 500)

  val route = 
    authenticate(BasicAuth(Security.adminPassAuthenticator _, realm = "secure site")) { userName =>
      pathSingleSlash{
         cache(simpleCache) {
            encodeResponse(Gzip){
              getFromResource("frontend/web/index.html")   
          }
        }
      } ~
      cache(simpleCache) {
        encodeResponse(Gzip){
          getFromResourceDirectory("frontend/web")
        }
      }  
    }    
}