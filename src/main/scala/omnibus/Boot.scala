package omnibus

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import omnibus.service.OmnibusBuilder

// Object used to bootstrap the standalone application
object Boot extends App {
  val log: Logger = LoggerFactory.getLogger("omnibus.boot")
  log.info("Booting Omnibus in standalone mode...")
  OmnibusBuilder.start()
}