package omnibus.core.actors

import akka.actor._
import omnibus.core.metrics.Instrumented

trait CommonActor extends Actor with ActorLogging with Instrumented {

  override def postRestart(reason: Throwable) {
    log.debug(s"Restarted actor: ${self.path}")
    super.postRestart(reason)
  }
}

