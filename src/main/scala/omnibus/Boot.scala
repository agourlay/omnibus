package omnibus

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File

import omnibus.service.OmnibusBuilder

// Object used to bootstrap the standalone application
object Boot extends App {
  val log: Logger = LoggerFactory.getLogger("omnibus.boot")
  log.info("Booting Omnibus in standalone mode...")
  val externalConfPath = "../conf/omnibus.conf"
  if (new File(externalConfPath).exists()){
  	System.setProperty("config.file", externalConfPath );
  	log.info(s"using external configuration file $externalConfPath")
  }
  OmnibusBuilder.start()
}