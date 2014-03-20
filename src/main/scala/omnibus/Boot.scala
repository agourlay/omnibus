package omnibus

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File

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
import omnibus.core._

object Boot extends App with BootedCore with CoreActors {

  val log: Logger = LoggerFactory.getLogger("omnibus.boot")
  log.info("Booting Omnibus in standalone mode...")
  val externalConfPath = "../conf/omnibus.conf"
  
  if (new File(externalConfPath).exists()){
  	System.setProperty("config.file", externalConfPath );
  	log.info(s"using external configuration file $externalConfPath")
  }
  
  implicit def executionContext = system.dispatcher

  val httpPort = Settings(system).Http.Port

  // HttpService actor exposing omnibus routes
  val omnibusHttp = system.actorOf(HttpEndpoint.props(httpStatService, topicRepo, subRepo), "omnibus-http")

  log.info(s"Omnibus starting on port $httpPort ~~> ")

  IO(Http) ! Http.Bind(omnibusHttp, "localhost", port = httpPort)
}