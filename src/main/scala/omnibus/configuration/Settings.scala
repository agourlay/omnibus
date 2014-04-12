package omnibus.configuration

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration
import com.typesafe.config.Config
import akka.actor._

class Settings(config: Config, extendedSystem: ExtendedActorSystem) extends Extension {

  object Http {
    val Port = config.getInt("omnibus.http.port")
  }

  object Admin {
    val Name = config.getString("omnibus.admin.userName")
    val Password = config.getString("omnibus.admin.password")
  }

  object Topic {
    val RetentionTime = FiniteDuration(config.getDuration("omnibus.topic.retentionTime", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
  }

  object Timeout {
  	val Ask = FiniteDuration(config.getDuration("omnibus.timeout.ask", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
  }

  object Graphite {
    val Enable = config.getBoolean("omnibus.graphite.enable")
    val Host = config.getString("omnibus.graphite.host")
    val Prefix = config.getString("omnibus.graphite.prefix")
  }

  object Cluster {
    val SeedNodes = config.getStringList("omnibus.cluster.nodes")
  }
}

object Settings extends ExtensionId[Settings] with ExtensionIdProvider {
  override def lookup = Settings
  override def createExtension(system: ExtendedActorSystem) = new Settings(system.settings.config, system)
}