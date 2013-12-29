package omnibus.configuration

import com.typesafe.config.ConfigFactory

trait Configuration {
  val systemName = "omnibus"
  val omnibusConfig = ConfigFactory.load().getConfig(systemName)
  val defaultHttpPort = omnibusConfig.getInt("default.http.port")
  val defaultHttpEnable = omnibusConfig.getBoolean("default.http.enable")
}