package omnibus

import akka.actor._
import akka.routing._
import akka.io.IO

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.postfixOps

import spray.can.Http

import omnibus.service.OmnibusRest
import omnibus.service.TopicService
import omnibus.service.SubscriptionService
import omnibus.domain.TopicRepository

object Boot extends App with Configuration{
 
  val log: Logger = LoggerFactory.getLogger("boot")
  log.info("Omnibus Starting... ")

  implicit val system = ActorSystem(systemName)
  implicit def executionContext = system.dispatcher
  val routerSize = Runtime.getRuntime.availableProcessors * 2

  // parent of the topic tree 
  val topicRepository = system.actorOf(Props(classOf[TopicRepository]), "topic-repository")
  
  // responsible for managing the topicRepository    
  val topicService = system.actorOf(Props(classOf[TopicService], topicRepository)
                           .withRouter(SmallestMailboxPool(routerSize)), "topic-service")

  // responsible for managing subscribers
  val subsService = system.actorOf(Props(classOf[SubscriptionService], topicService)
                          .withRouter(SmallestMailboxPool(routerSize)), "subscription-service")

  val httpService = system.actorOf(Props(classOf[OmnibusRest], topicService, subsService), "http-service")
  
  IO(Http) ! Http.Bind(httpService, "localhost", port = port) 
}