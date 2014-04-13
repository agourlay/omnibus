package omnibus

import omnibus.configuration.Configuration
import omnibus.api.Web
import omnibus.api.Rest
import omnibus.core.BootedCore
import omnibus.core.CoreActors

object Boot extends App with BootedCore with Configuration with CoreActors with Rest with Web {}