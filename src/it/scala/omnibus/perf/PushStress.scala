package omnibus.it.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

import omnibus.it.OmnibusSimulation

class PushStress extends OmnibusSimulation {
  
  val userNumber = 10

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

  val scenarioOmnibus = scenario("Publish on topic")
    .pause(6)
    .exec(
      http("get wrong topic")
        .get("/topics/batmans")
        .check(status.is(404)))
    .exec(
      http("get proper topic")
        .get("/topics/batman")
        .check(status.is(200)))
    .pause(1)
    .repeat(100) {
      exec(
        http("push on topic")
          .put("/topics/batman")
          .body(StringBody("Na na na na na na na na na na na na na na na na... BATMAN!"))
          .check(status.is(202)))
        .exec(
          http("topic stats")
            .get("/stats/topics/batman")
            .check(status.is(200)))
        .exec(
          http("server metrics")
            .get("/stats/metrics")
            .check(status.is(200)))
    }

  setUp(
    scenarioCreateTopic.inject(atOnceUsers(1)),
    scenarioOmnibus.inject(rampUsers(userNumber) over (10 seconds)))
    .protocols(
      http.baseURL("http://localhost:8080")
       .warmUp("http://localhost:8080/stats/metrics")
    )
    .assertions(
      global.successfulRequests.percent.greaterThan(minSuccessPercentage),
      global.responseTime.percentile1.lessThan(maxResponseTimePercentile1)
    )
}
