package omnibus.it.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import util.Random

import omnibus._

class DeepTopicTree extends Simulation {

  // starting app
  val app = omnibus.Boot
  val depth = 10
  val topicNameLength = 5

  def randomTopic(depth: Int): String = {
    val topic = "/" + Random.alphanumeric.take(topicNameLength).mkString
    if (depth == 1) topic else topic + randomTopic(depth - 1)
  }

  val scenarioOmnibus = scenario("Publish on topic")
    .exec(session => session.set("topicName", randomTopic(depth)))
    .exec(
      http("create random topic")
        .post("/topics${topicName}")
        .check(status.is(201)))

  setUp(
    scenarioOmnibus.inject(rampUsers(10) over (10 seconds)))
    .protocols(
      http.baseURL("http://localhost:8080")
    )
    .assertions(
      global.successfulRequests.percent.greaterThan(95), global.responseTime.max.lessThan(400))
}
