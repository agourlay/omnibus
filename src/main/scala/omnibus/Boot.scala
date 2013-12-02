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

object Boot extends App with Configuration{
 
  val log: Logger = LoggerFactory.getLogger("boot")
  log.info("Omnibus Starting... ")

  implicit val system = ActorSystem(systemName)
  implicit def executionContext = system.dispatcher
      
  val topicService = system.actorOf(Props(classOf[TopicService])
                          .withRouter(SmallestMailboxRouter(Runtime.getRuntime.availableProcessors)), "topic-service")

  val subscriptionService = system.actorOf(Props(classOf[SubscriptionService])
                          .withRouter(SmallestMailboxRouter(Runtime.getRuntime.availableProcessors)), "subscription-service")

  val httpService = system.actorOf(Props(classOf[OmnibusRest], topicService, subscriptionService), "http-service")
  
  IO(Http) ! Http.Bind(httpService, "localhost", port = port) 
}