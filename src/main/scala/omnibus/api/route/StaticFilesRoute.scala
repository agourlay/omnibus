package omnibus.api.route

import akka.actor.{ Actor, ActorRef, Props, ActorContext }

import spray.httpx.encoding._
import spray.routing._
import spray.routing.authentication._

import omnibus.configuration.Security

class StaticFilesRoute(implicit context: ActorContext) extends Directives {

  implicit def executionContext = context.dispatcher

  val route =
    authenticate(BasicAuth(Security.adminPassAuthenticator _, realm = "secure site")) { userName ⇒
      pathSingleSlash {
        encodeResponse(Gzip) {
          getFromResource("frontend/web/dist/index.html")
        }
      } ~
        encodeResponse(Gzip) {
          getFromResourceDirectory("frontend/web/dist")
        }
    }
}