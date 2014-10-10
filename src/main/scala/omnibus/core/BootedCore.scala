package omnibus.core

import akka.actor.ActorSystem

trait Core {
  implicit def system: ActorSystem
  implicit def systemWS: ActorSystem
}

trait BootedCore extends Core {
  //spray-websocket requires a dedicated ActorSystem, it should be fixed by akka-http	
  implicit lazy val system = ActorSystem("omnibus")
  implicit lazy val systemWS = ActorSystem("websocket")
  sys.addShutdownHook(system.shutdown())
  sys.addShutdownHook(systemWS.shutdown())
}