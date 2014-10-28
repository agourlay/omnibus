package omnibus.it.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

import omnibus.it.OmnibusSimulation

class PushAndSubStress extends OmnibusSimulation {
  
  val userNumber = 10
  val publishNumber = 100

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

  val scenarioListenerTopic = scenario("Global listener")
    .pause(6)
    .exec(ws("Subscribe to topic").open("/streams/topics/batman")
      .check(wsListen.within(30 seconds).expect(publishNumber * userNumber)))

  val scenarioOmnibus = scenario("Sub and push")
    .pause(6)
    .exec(ws("Subscribe to topic").open("/streams/topics/batman"))
    .repeat(publishNumber) {
      exec(
        http("push on topic")
          .put("/topics/batman")
          .body(StringBody("Na na na na na na na na na na na na na na na na... BATMAN!"))
      )
    }

  setUp(
    scenarioCreateTopic.inject(atOnceUsers(1)),
    scenarioListenerTopic.inject(atOnceUsers(1)),
    scenarioOmnibus.inject(rampUsers(userNumber) over (30 seconds)))
    .protocols(
      http.baseURL("http://localhost:8080")
        .wsBaseURL("ws://localhost:8081")
         .warmUp("http://localhost:8080/stats/metrics")
    )
    .assertions(
      global.successfulRequests.percent.greaterThan(minSuccessPercentage),
      global.responseTime.percentile1.lessThan(maxResponseTimePercentile1)
    )
}
