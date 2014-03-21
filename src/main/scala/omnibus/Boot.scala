package omnibus

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.language.postfixOps

import omnibus.api._
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