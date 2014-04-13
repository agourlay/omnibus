package omnibus.core
 
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor._

import scala.collection.JavaConversions._

import omnibus.configuration.Settings
 
class ClusterListener extends Actor with ActorLogging {
 
  val system = context.system
  val cluster = Cluster(system)

  override def preStart() = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
  }

  val seedNodes = Settings(system).Cluster.SeedNodes.toList

  if (!seedNodes.isEmpty && !seedNodes.contains("")) {
    log.info("Trying to join seed nodes: {}", seedNodes)
    Cluster(system).joinSeedNodes(seedNodes.map(nodeAddress(_)))
  } else {
    Cluster(system).join(Cluster(system).selfAddress)
  }

  override def postStop() = cluster.unsubscribe(self)
 
  def receive = {
    case MemberUp(member)                      => log.info("Member is Up: {}", member.address)
    case UnreachableMember(member)             =>  log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) => log.info("Member is Removed: {} after {}", member.address, previousStatus)
    case m: MemberEvent                        => log.info("MemberEvent: {}", m)
  }

  def nodeAddress(conf : String) = {
    Address("akka.tcp","omnibus", conf.split(":")(0), conf.split(":")(1).toInt)
  }  
}

object ClusterListener {
  def props = Props(classOf[ClusterListener])
}