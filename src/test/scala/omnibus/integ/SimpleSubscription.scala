package omnibus.test.integ

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

import omnibus._

class SimpleSubscription extends Simulation {

  // starting app
  val app = omnibus.Boot
  val publishNumber = 100

  val scenarioOmnibus = scenario("Test simple subscription")
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
    )
    .assertions(
      global.successfulRequests.percent.greaterThan(95))
}
