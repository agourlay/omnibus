package omnibus.service

import akka.actor._
import akka.io.IO

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.language.postfixOps

import spray.can.Http

import omnibus.http._
import omnibus.repository._
import omnibus.configuration._
import omnibus.http.stats._

object OmnibusBuilder {

  def start() = {

    implicit val system = ActorSystem("omnibus")
    implicit def executionContext = system.dispatcher

    val log: Logger = LoggerFactory.getLogger("omnibusBuilder")

    val httpPort = Settings(system).Http.Port

    // parent of the topic tree 
    val topicRepository = system.actorOf(TopicRepository.props, "topic-repository")

    // parent of the subscriber tree
    val subRepository = system.actorOf(SubscriberRepository.props, "subscriber-repository")

    // http stats
    val httpStatService = system.actorOf(HttpStatistics.props, "http-stat-service")

    // HttpService actor exposing omnibus routes
    val omnibusHttp = system.actorOf(HttpEndpoint.props(httpStatService, topicRepository, subRepository), "omnibus-http")

    log.info(s"Omnibus starting on port $httpPort ~~> ")

    IO(Http) ! Http.Bind(omnibusHttp, "localhost", port = httpPort)
  }
}