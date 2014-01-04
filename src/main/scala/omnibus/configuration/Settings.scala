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

  object Data {
    val RetentionTime = FiniteDuration(config.getMilliseconds("omnibus.data.retentionTime"), TimeUnit.MILLISECONDS)
  }

  object Timeout {
  	val Ask = FiniteDuration(config.getMilliseconds("omnibus.timeout.ask"), TimeUnit.MILLISECONDS)
  }

  object Statistics {
    val StorageInterval = FiniteDuration(config.getMilliseconds("omnibus.statistics.storageInterval"), TimeUnit.MILLISECONDS)
    val PushInterval = FiniteDuration(config.getMilliseconds("omnibus.statistics.pushInterval"), TimeUnit.MILLISECONDS)
    val RetentionTime = FiniteDuration(config.getMilliseconds("omnibus.statistics.retentionTime"), TimeUnit.MILLISECONDS)
  }
}


object Settings extends ExtensionId[Settings] with ExtensionIdProvider {
  override def lookup = Settings
  override def createExtension(system: ExtendedActorSystem) = new Settings(system.settings.config, system)
}