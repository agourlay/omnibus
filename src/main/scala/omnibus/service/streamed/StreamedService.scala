package omnibus.service.streamed

import akka.actor._

import omnibus.core.actors.CommonActor
import omnibus.configuration._

class StreamedService(replyTo: ActorRef) extends CommonActor {

  implicit def system = context.system
  implicit val timeout = akka.util.Timeout(Settings(system).Timeout)

  context.setReceiveTimeout(timeout.duration)

  override def receive: Receive = {
    case ReceiveTimeout ⇒
      replyTo ! TimeoutStream
      self ! PoisonPill
  }
}

case object EndOfStream
case object TimeoutStream
