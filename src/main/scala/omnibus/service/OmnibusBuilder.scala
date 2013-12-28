package omnibus.service

import akka.actor._
import akka.routing._
import akka.io.IO

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.postfixOps

import spray.can.Http

import omnibus.http._
import omnibus.service._
import omnibus.repository._
import omnibus.configuration._

object OmnibusBuilder extends Configuration {

  def start(httpPort : Int = defaultHttpPort) : OmnibusReceptionist = {

    implicit val system = ActorSystem(systemName)
    implicit def executionContext = system.dispatcher

    val log: Logger = LoggerFactory.getLogger("omnibusBuilder")  

    // parent of the topic tree 
    val topicRepository = system.actorOf(Props(classOf[TopicRepository]), "topic-repository")

    // parent of the subscriber tree
    val subRepository = system.actorOf(Props(classOf[SubscriberRepository]), "subscriber-repository")
    
    // awesome service layer
    val omnibusService = system.actorOf(Props(classOf[OmnibusService], topicRepository, subRepository), "omnibus-service")

    val httpService = system.actorOf(Props(classOf[OmnibusRest], omnibusService), "omnibus-http")
    
    log.info(s"Omnibus starting on port $httpPort ~~> ")

    IO(Http) ! Http.Bind(httpService, "localhost", port = httpPort)
    
    new OmnibusReceptionist(system, omnibusService) 
  }
}