package omnibus

import omnibus.api._
import omnibus.core._
import omnibus.configuration.Configuration

object Boot extends App with BootedCore with Configuration with CoreActors with Rest with Web {}