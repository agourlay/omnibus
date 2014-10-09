package omnibus

import omnibus.configuration.Configuration
import omnibus.api.Api
import omnibus.core.BootedCore
import omnibus.core.CoreActors

object Boot extends App with BootedCore with Configuration with CoreActors with Api {}