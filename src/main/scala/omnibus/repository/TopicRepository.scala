package omnibus.repository

import akka.actor._

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util._

import omnibus.domain._
import omnibus.repository.TopicRepositoryProtocol._

class TopicRepository extends Actor with ActorLogging {

  implicit def executionContext = context.dispatcher

  var rootTopics : Map[String, ActorRef] = Map.empty[String, ActorRef]

  def receive = {
    case CreateTopicActor(topic)             => createTopicActor(topic)
    case DeleteTopicActor(topic)             => deleteTopicActor(topic)
    case CheckTopicActor(topic)              => sender ! checkTopicActor(topic)
    case PublishToTopicActor(topic, message) => publishToTopicActor(topic, message)
  }

  def createTopicActor(topicName : String) = {
    val topicsList = topicName.split("/").toList
    val topicRoot = topicsList.head

    if (rootTopics.contains(topicRoot)) rootTopics(topicRoot) ! TopicProtocol.CreateSubTopic(topicsList.tail)
    else rootTopics += (topicRoot -> context.actorOf(Props(classOf[Topic], topicRoot)))
    
  }

  def publishToTopicActor(topicName : String, message : String) = {
    lookupTopic(topicName) match {
      case Some(topicRef) => topicRef ! TopicProtocol.PublishMessage( new Message(topicName, message) )
      case None           => log.info(s"trying to push to non existing topic $topicName")
    }
  }

  def lookupTopic(topic : String) : Option[ActorRef] = {
    implicit val timeout = akka.util.Timeout(1 seconds)
    val selection : Future[ActorRef] = context.actorSelection(topic).resolveOne
    val topicRef : Try[ActorRef] = selection.value.get
    topicRef match {
      case Success(ref) => Some(ref)
      case Failure(err) => None  
    }
  }

  def deleteTopicActor(topicName : String) = {
    lookupTopic(topicName) match {
      case Some(topicRef) => topicRef ! PoisonPill
      case None           => log.info(s"trying to delete non existing topic $topicName")
    }
  }

  def checkTopicActor(topicName : String) : Boolean = {
    lookupTopic(topicName) match {
      case Some(topicRef) => true
      case None           => false
    }
  }
}

object TopicRepositoryProtocol {
  case class CreateTopicActor(topicName : String)
  case class DeleteTopicActor(topicName : String)
  case class CheckTopicActor(topicName : String)
  case class PublishToTopicActor(topicName : String, message : String)
}