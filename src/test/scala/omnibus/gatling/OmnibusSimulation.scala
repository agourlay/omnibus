/*package omnibus.test.gatling

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

import omnibus.service._

!! UNCOMMENT WHEN GATLING UPDATES AKKA TO 2.3 !!

class OmnibusSimulation extends TestableSimulation {
  // Boot!
  OmnibusBuilder.start()

  val rootScenario = scenario("look for the root topics")
    .exec(
      http("topics")
        .get("/topics")
        .check(status.is(200))
    )

  setUp(rootScenario.inject(rampUsers(100).over(5 seconds)))
    .protocols(http.baseURL("http://localhost:8080"))
    .assertions(
      global.responseTime.max.lessThan(100),
      global.failedRequests.count.is(0)
    )
} */   

