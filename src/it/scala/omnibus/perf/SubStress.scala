package omnibus.it.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

import omnibus.it.OmnibusSimulation

class SubStress extends OmnibusSimulation {

  val scenarioCreateTopic = scenario("Create topic")
    .pause(5)
    .exec(
      http("create topic")
        .post("/topics/batman")
        .check(status.is(201)))
    .exec(
      http("topic existence")
        .get("/topics/batman")
        .check(status.is(200)))

  val scenarioOmnibus = scenario("Subcription")
    .pause(6)
    .exec(ws("Subscribe to topic").open("/streams/topics/batman"))

  setUp(
    scenarioCreateTopic.inject(atOnceUsers(1)),
    scenarioOmnibus.inject(rampUsers(100) over (10 seconds)))
    .protocols(
      http.baseURL("http://localhost:8080")
        .wsBaseURL("ws://localhost:8081")
        .warmUp("http://localhost:8080/stats/metrics")
    )
    .assertions(
      global.successfulRequests.percent.greaterThan(miniSuccessPercentage),
      global.responseTime.max.lessThan(maxResponseTime)
    )
}
