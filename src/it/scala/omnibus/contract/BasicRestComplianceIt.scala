package omnibus.it.contract

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

import omnibus.it.OmnibusSimulation

class BasicRestComplianceIt extends OmnibusSimulation {

  val scenarioCreateTopic = scenario("Create topic")
    .pause(5)
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
      http("topic roots")
        .get("/topics")
        .check(status.is(200)))
    .exec(
      http("wrong topic stats")
        .get("/stats/topics/batmans")
        .check(status.is(404)))
    .exec(
      http("push on topic")
        .put("/topics/batman")
        .body(StringBody("Na na na na na na na na na na na na na na na na... BATMAN!"))
        .check(status.is(202)))
    .exec(
      http("push on wrong topic")
        .put("/topics/batmans")
        .body(StringBody("Na na na na na na na na na na na na na na na na... BATMAN!"))
        .check(status.is(404)))
    .exec(
      http("server metrics")
        .get("/stats/metrics")
        .check(status.is(200)))
    .exec(
      http("subscriber list")
        .delete("/admin/subscribers")
        .basicAuth(httpBasicUser, httpBasicPwd)
        .check(status.is(200)))
    .exec(
      http("delete topic")
        .delete("/admin/topics/batman")
        .basicAuth(httpBasicUser, httpBasicPwd)
        .check(status.is(202)))
    .exec(
      http("topic deleted")
        .get("/topics/batman")
        .check(status.is(404)))

  setUp(
    scenarioCreateTopic.inject(atOnceUsers(1)))
    .protocols(
      http.baseURL("http://localhost:8080")
      .warmUp("http://localhost:8080/stats/metrics")
    )
    .assertions(
      global.responseTime.max.lessThan(maxResponseTime),
      global.successfulRequests.percent.greaterThan(miniSuccessPercentage)
    )
}
