package omnibus

import akka.actor._
import akka.routing._
import akka.io.IO

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.postfixOps

import spray.can.Http

import omnibus.service._
import omnibus.repository._

object Boot extends App with Configuration{
 
  val log: Logger = LoggerFactory.getLogger("boot")
  log.info("Omnibus Starting... ")

  implicit val system = ActorSystem(systemName)
  implicit def executionContext = system.dispatcher
  val routerSize = Runtime.getRuntime.availableProcessors * 2

  // parent of the topic tree 
  val topicRepository = system.actorOf(Props(classOf[TopicRepository]), "topic-repository")

  // parent of the subscriber tree
  val subRepository = system.actorOf(Props(classOf[SubscriberRepository]), "subscriber-repository")
  
  val omnibusService = system.actorOf(Props(classOf[OmnibusService], topicRepository, subRepository)
                           .withRouter(SmallestMailboxPool(routerSize)), "omnibus-service")

  val httpService = system.actorOf(Props(classOf[OmnibusRest], omnibusService), "omnibus-http")
  
  IO(Http) ! Http.Bind(httpService, "localhost", port = port) 
}