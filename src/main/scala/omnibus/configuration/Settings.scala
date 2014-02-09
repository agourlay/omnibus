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

  object Statistics {
    val StorageInterval = FiniteDuration(config.getDuration("omnibus.statistics.storageInterval", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
    val PushInterval = FiniteDuration(config.getDuration("omnibus.statistics.pushInterval", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
    val RetentionTime = FiniteDuration(config.getDuration("omnibus.statistics.retentionTime", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
    val Resolution = FiniteDuration(config.getDuration("omnibus.statistics.resolution", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
  }
}

object Settings extends ExtensionId[Settings] with ExtensionIdProvider {
  override def lookup = Settings
  override def createExtension(system: ExtendedActorSystem) = new Settings(system.settings.config, system)
}