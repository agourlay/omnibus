package omnibus.test.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

import omnibus._

class BasicRestCompliance extends Simulation {

  // starting app
  val app = omnibus.Boot

  val scenarioCreateTopic = scenario("Create topic")
    .exec(
      http("create topic")
        .post("/topics/batman")
        .check(status.is(201)))
    .exec(
      http("create topic twice")
        .post("/topics/batman")
        .check(status.is(202)))
    .exec(
      http("topic existence")
        .get("/topics/batman")
        .check(status.is(200)))
    .exec(
      http("get wrong topic")
        .get("/topics/batmans")
        .check(status.is(404)))
    .exec(
      http("topic stats")
        .get("/stats/topics/batman")
        .check(status.is(200)))
    .exec(
      http("wrong topic stats")
        .get("/stats/topics/batmans")
        .check(status.is(404)))
  exec(
    http("push on topic")
      .put("/topics/batman")
      .body(StringBody("Na na na na na na na na na na na na na na na na... BATMAN!"))
      .check(status.is(202)))
  exec(
    http("push on wrong topic")
      .put("/topics/batmans")
      .body(StringBody("Na na na na na na na na na na na na na na na na... BATMAN!"))
      .check(status.is(404)))
    .exec(
      http("server metrics")
        .get("/stats/metrics")
        .check(status.is(200)))

  setUp(
    scenarioCreateTopic.inject(atOnceUsers(1)))
    .protocols(
      http.baseURL("http://localhost:8080")
    )
    .assertions(
      global.responseTime.max.lessThan(200),
      global.successfulRequests.percent.is(100))
}
