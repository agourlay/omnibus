package omnibus

import com.typesafe.config.ConfigFactory

trait Configuration {
  val systemName    = "omnibus"
  val omnibusConfig = ConfigFactory.load().getConfig(systemName)
  val port          = omnibusConfig.getInt("port")
}