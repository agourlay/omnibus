package omnibus.service

import akka.actor._
import akka.pattern._
import akka.routing._

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps

import omnibus.service._
import omnibus.configuration._
import omnibus.service.OmnibusServiceProtocol._
import omnibus.repository._
import omnibus.domain._
import omnibus.domain.subscriber._

class OmnibusReceptionist(system: ActorSystem, omnibusService: ActorRef) {
  implicit def executionContext = system.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(system).Timeout.Ask)

  val log: Logger = LoggerFactory.getLogger("OmnibusReceptionist")

  def createTopic(topic: String, message: String) = {
    omnibusService ! OmnibusServiceProtocol.CreateTopic(topic, message)
  }

  def deleteTopic(topic: String) = omnibusService ! OmnibusServiceProtocol.DeleteTopic(topic)

  def checkTopic(topic: String): Future[Boolean] = {
    (omnibusService ? OmnibusServiceProtocol.CheckTopic(topic)).mapTo[Boolean]
  }

  def publishToTopic(topic: String, message: String) = {
    omnibusService ! OmnibusServiceProtocol.PublishToTopic(topic, message)
  }

  def subToTopic(topic: String, subscriber: ActorRef, mode: ReactiveCmd) = {
    omnibusService ! OmnibusServiceProtocol.SubToTopic(topic, subscriber, mode, false)
  }

  def unsubFromTopic(topic: String, subscriber: ActorRef) = {
    omnibusService ! OmnibusServiceProtocol.UnsubFromTopic(topic, subscriber)
  }

  def shutDownOmnibus() = system.shutdown
}