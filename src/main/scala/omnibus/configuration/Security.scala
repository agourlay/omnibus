package omnibus.configuration

import akka.actor.ActorContext
import spray.routing.authentication._
import scala.concurrent.Future

object Security {
  def adminPassAuthenticator(userPass: Option[UserPass])(implicit context: ActorContext) = {
    implicit val system = context.system
    implicit val executionContext = context.dispatcher

    Future {
      if (userPass.exists(up ⇒ up.user == Settings(system).Admin.Name && up.pass == Settings(system).Admin.Password))
        Some(Settings(system).Admin.Name)
      else None
    }
  }
}