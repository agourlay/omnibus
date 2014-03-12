package omnibus

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import io.gatling.http.Headers.Names._
import scala.concurrent.duration._
import bootstrap._
import assertions._

class PushSimulation extends Simulation {

	val scenarioOmnibus = scenario("Push like crazy")
		.repeat(100){
			exec(
				http("push on batman")
					.put("/topics/batman")
					.body(StringBody("Na na na na na na na na na na na na na na na na... BATMAN!"))
					.check(status.is(202)))
		}

	setUp(scenarioOmnibus.inject(ramp(1000 users) over (30 seconds)))
		.protocols(
			       http.baseURL("http://localhost:8080")
			       .warmUp("http://localhost:8080/topics/batman")
			       )
		.assertions(
			global.successfulRequests.percent.is(100)
		  , global.responseTime.max.lessThan(2000)
		  ,	global.requestsPerSec.greaterThan(200))
}
