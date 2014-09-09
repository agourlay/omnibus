package omnibus.test.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

import omnibus._

class PushSimulation extends Simulation {

	// starting app
	val app = omnibus.Boot

	val scenarioCreateTopic = scenario("Create topic")
		.exec(
			http("create topic batman")
			    .post("/topics/batman")
			    .check(status.is(201)))
		.exec(
			http("create topic batman")
			    .get("/topics/batman")
				.check(status.is(200)))


	val scenarioOmnibus = scenario("Publish on topic")
		.pause(1)
		.repeat(100){
			exec(
				http("push on batman")
					.put("/topics/batman")
					.body(StringBody("Na na na na na na na na na na na na na na na na... BATMAN!"))
					.check(status.is(202)))
		}

	setUp(
		scenarioCreateTopic.inject(atOnceUsers(1)),
		scenarioOmnibus.inject(rampUsers(100) over (30 seconds)))
		.protocols(
			       http.baseURL("http://localhost:8080")
			       )
		.assertions(
			global.successfulRequests.percent.is(100)
		  , global.responseTime.max.lessThan(500)
		  ,	global.requestsPerSec.greaterThan(300))
}
