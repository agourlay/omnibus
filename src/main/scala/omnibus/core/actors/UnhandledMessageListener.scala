package omnibus.core.actors

import akka.actor._

class UnhandledMessageListener extends CommonActor {

  context.system.eventStream.subscribe(self, classOf[UnhandledMessage])

  val unhandledReceived = metrics.meter("events")

  override def receive = {
    case message: UnhandledMessage ⇒
      unhandledReceived.mark()
      log.error(s"actor did not handle message ${message.getMessage}")
  }
}

object UnhandledMessageListener {
  def props = Props(classOf[UnhandledMessageListener])
}