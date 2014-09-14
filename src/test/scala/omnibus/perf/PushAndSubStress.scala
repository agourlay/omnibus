package omnibus.test.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

import omnibus._

class PushAndSubStress extends Simulation {

  // starting app
  val app = omnibus.Boot
  val userNumber = 100
  val publishNumber = 1000

  val scenarioCreateTopic = scenario("Create topic")
    .exec(
      http("create topic")
        .post("/topics/batman")
        .check(status.is(201)))
    .exec(
      http("topic existence")
        .get("/topics/batman")
        .check(status.is(200)))

  val scenarioListenerTopic = scenario("Global listener")
    .exec(ws("Subscribe to topic").open("/streams/topics/batman")
      .check(wsListen.within(200 seconds).expect(publishNumber * userNumber)))

  val scenarioOmnibus = scenario("Sub and push")
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
    scenarioOmnibus.inject(rampUsers(userNumber) over (60 seconds)))
    .protocols(
      http.baseURL("http://localhost:8080")
        .wsBaseURL("ws://localhost:8081")
    )
    .assertions(
      global.successfulRequests.percent.is(100), global.responseTime.max.lessThan(500))
}
