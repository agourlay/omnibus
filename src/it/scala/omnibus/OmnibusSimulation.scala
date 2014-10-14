package omnibus.it

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

import omnibus._

trait OmnibusSimulation extends Simulation {

	val miniSuccessPercentage = 95
	val maxResponseTime = 500

	before {
	  // starting app
	  omnibus.Boot
	}

	after {

	}

}