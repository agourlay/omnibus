package omnibus.core

import akka.actor.ActorSystem

trait Core {
  implicit def system: ActorSystem
  implicit def systemWS: ActorSystem
}

trait BootedCore extends Core {
  implicit lazy val system = ActorSystem("omnibus")
  implicit lazy val systemWS = ActorSystem("websocket")
  sys.addShutdownHook(system.shutdown())
  sys.addShutdownHook(systemWS.shutdown())
}