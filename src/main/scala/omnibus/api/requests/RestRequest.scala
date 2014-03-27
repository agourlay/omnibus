package omnibus.api.request

import akka.actor._

import spray.routing._

import omnibus.configuration._
import omnibus.api.exceptions.RestRequestTimeoutException
import omnibus.api.request.RestRequestProtocol._

class RestRequest(ctx : RequestContext) extends Actor with ActorLogging {

  implicit def system = context.system
  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(system).Timeout.Ask)

  system.scheduler.scheduleOnce(timeout.duration, self, RestRequestProtocol.RequestTimeout)
  
  def receive = handleTimeout

  def handleTimeout : Receive = {
    case RequestTimeout => {
      ctx.complete(new RestRequestTimeoutException())
      self ! PoisonPill
    }  
  }
}

object RestRequestProtocol {
  case object RequestTimeout
}