package omnibus.configuration

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration
import com.typesafe.config.Config
import akka.actor._

class Settings(config: Config, extendedSystem: ExtendedActorSystem) extends Extension {

  val Timeout = FiniteDuration(config.getDuration("omnibus.timeout", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)

  object Http {
    val Port = config.getInt("omnibus.http.port")
  }

  object Websocket {
    val Port = config.getInt("omnibus.websocket.port")
    val Enable = config.getBoolean("omnibus.websocket.enable")
  }

  object Admin {
    val Name = config.getString("omnibus.admin.userName")
    val Password = config.getString("omnibus.admin.password")
  }

  object Topic {
    val RetentionTime = FiniteDuration(config.getDuration("omnibus.topic.retentionTime", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
  }

  object Graphite {
    val Enable = config.getBoolean("omnibus.graphite.enable")
    val Host = config.getString("omnibus.graphite.host")
    val Port = config.getInt("omnibus.graphite.port")
    val Prefix = config.getString("omnibus.graphite.prefix")
  }
}

object Settings extends ExtensionId[Settings] with ExtensionIdProvider {
  override def lookup = Settings
  override def createExtension(system: ExtendedActorSystem) = new Settings(system.settings.config, system)
}