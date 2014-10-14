package omnibus.it.contract

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

import omnibus.it.OmnibusSimulation

class SimpleSubscriptionIt extends OmnibusSimulation {
  
  val publishNumber = 100

  val scenarioOmnibus = scenario("Test simple subscription")
    .pause(5)
    .exec(
      http("create topic")
        .post("/topics/batman")
        .check(status.is(201)))
    .exec(
      http("topic existence")
        .get("/topics/batman")
        .check(status.is(200)))
    .exec(ws("Subscribe to topic").open("/streams/topics/batman")
      .check(wsListen.within(20 seconds).until(publishNumber)))
    .repeat(publishNumber) {
      exec(
        http("push on topic")
          .put("/topics/batman")
          .body(StringBody("Na na na na na na na na na na na na na na na na... BATMAN!"))
      )
    }

  setUp(scenarioOmnibus.inject(atOnceUsers(1)))
    .protocols(
      http.baseURL("http://localhost:8080")
        .wsBaseURL("ws://localhost:8081")
        .warmUp("http://localhost:8080/stats/metrics")
    )
    .assertions(
      global.successfulRequests.percent.greaterThan(miniSuccessPercentage)
    )
}
