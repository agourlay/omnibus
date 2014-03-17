package omnibus.http.route

import akka.actor._

import spray.httpx.encoding._
import spray.routing._
import spray.routing.authentication._
import spray.routing.directives.CachingDirectives._

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import omnibus.configuration._

class AdminUIRoute(implicit context: ActorContext) extends Directives {

  implicit def executionContext = context.dispatcher

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