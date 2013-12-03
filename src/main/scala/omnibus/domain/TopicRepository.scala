package omnibus.domain

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps

import omnibus.domain.TopicRepositoryProtocol._

class TopicRepository extends Actor with ActorLogging {

  implicit def executionContext = context.dispatcher

  def receive = {
    case CreateTopicActor(topic) => sender ! "Yo"
  }

  def checkActorExist(topicName : String) = {
  	
  }

}

object TopicRepositoryProtocol {
  case class CreateTopicActor(topicName : String)
  case class DeleteTopicActor(topicName : String)
  case class CheckTopicActor(topicName : String)
  case class PublishToTopicActor(topicName : String, message : String)
}