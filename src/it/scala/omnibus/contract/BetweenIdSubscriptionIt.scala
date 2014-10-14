package omnibus.it.contract

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

import omnibus.it.OmnibusSimulation

class BetweenIdSubscriptionIt extends OmnibusSimulation {
  
  val publishNumber = 100

  val scenarioOmnibus = scenario("Test betweenId")
    .pause(5)
    .exec(
      http("create topic")
        .post("/topics/batman")
        .check(status.is(201)))
    .exec(
      http("topic existence")
        .get("/topics/batman")
        .check(status.is(200)))
    .repeat(publishNumber) {
      exec(
        http("push on topic")
          .put("/topics/batman")
          .body(StringBody("Na na na na na na na na na na na na na na na na... BATMAN!"))
      )
    }
    .exec(ws("Subscribe to topic").open("/streams/topics/batman?react=between-id&since=10&to=50")
      .check(wsAwait.within(5 seconds).expect(41)))

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
