package omnibus.test.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

import omnibus._

class SubStress extends Simulation {

  // starting app
  val app = omnibus.Boot

  val scenarioCreateTopic = scenario("Create topic")
    .exec(
      http("create topic")
        .post("/topics/batman")
        .check(status.is(201)))
    .exec(
      http("topic existence")
        .get("/topics/batman")
        .check(status.is(200)))

  val scenarioOmnibus = scenario("Subcription")
    .exec(ws("Subscribe to topic").open("/streams/topics/batman"))

  setUp(
    scenarioCreateTopic.inject(atOnceUsers(1)),
    scenarioOmnibus.inject(rampUsers(100) over (10 seconds)))
    .protocols(
      http.baseURL("http://localhost:8080")
        .wsBaseURL("ws://localhost:8081")
    )
    .assertions(
      global.successfulRequests.percent.is(100), global.responseTime.max.lessThan(500))
}
