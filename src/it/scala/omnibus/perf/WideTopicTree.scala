package omnibus.it.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import util.Random

import omnibus.it.OmnibusSimulation

class WideTopicTree extends OmnibusSimulation {
  
  val width = 10
  val topicNameLength = 5

  def randomTopic(length: Int): String = "/" + Random.alphanumeric.take(topicNameLength).mkString

  val scenarioOmnibus = scenario("Publish on topic")
    .pause(5)
    .exec(session => session.set("topicBase", randomTopic(topicNameLength)))
    .exec(
      http("create random topic")
        .post("/topics${topicBase}")
        .check(status.is(201)))
    .repeat(100, "i") {
      exec(
        http("create random topic")
          .post("/topics${topicBase}/${i}")
          .check(status.is(201)))
    }

  setUp(
    scenarioOmnibus.inject(rampUsers(10) over (10 seconds)))
    .protocols(
      http.baseURL("http://localhost:8080")
       .warmUp("http://localhost:8080/stats/metrics")
    )
    .assertions(
      global.successfulRequests.percent.greaterThan(minSuccessPercentage),
      global.responseTime.percentile1.lessThan(maxResponseTimePercentile1)
    )
}
