package omnibus.test.api

import akka.testkit._
import akka.actor._

import spray.testkit.Specs2RouteTest
import spray.routing.Directives
import spray.http.HttpResponse

import org.specs2.mutable.Specification

import omnibus.api.endpoint._
import omnibus.core._

class HttpEndpointTest extends Specification with Specs2RouteTest with HttpEndpoint {

  def actorRefFactory = system

  "OmnibusRoute" should {}
}