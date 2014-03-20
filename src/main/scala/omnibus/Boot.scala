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

object Boot extends App with BootedCore with CoreActors with Rest with Web{

  val log: Logger = LoggerFactory.getLogger("omnibus.boot")
  log.info("Booting Omnibus in standalone mode...")
  val externalConfPath = "../conf/omnibus.conf"

  if (new File(externalConfPath).exists()){
  	System.setProperty("config.file", externalConfPath );
  	log.info(s"using external configuration file $externalConfPath")
  }
  log.info(s"Omnibus starting on port $httpPort ~~> ")
}