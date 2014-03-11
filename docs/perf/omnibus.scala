package omnibus

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import io.gatling.http.Headers.Names._
import scala.concurrent.duration._
import bootstrap._
import assertions._

class AdvancedExampleSimulation extends Simulation {

	val httpProtocol = http.baseURL("http://localhost:8080")

	val scenarioOmnibus = scenario("Push like crazy")
		.repeat(100){
			exec(
				http("push on batman")
					.put("/topics/batman")
					.body(StringBody("nya"))
					.check(status.is(202)))
		}

	setUp(scenarioOmnibus.inject(ramp(1000 users) over (30 seconds)))
		.protocols(httpProtocol)
		.assertions(
			global.successfulRequests.percent.is(100)
		  , details("push on batman").responseTime.max.lessThan(2000)
		  ,	details("push on batman").requestsPerSec.greaterThan(200))
}
