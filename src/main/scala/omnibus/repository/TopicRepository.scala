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
    case LookupTopicActor(topic)             => sender ! lookUpTopicActor(topic)
    case PublishToTopicActor(topic, message) => publishToTopicActor(topic, message)
  }

  def createTopicActor(topicName : String) = {
    val topicsList = topicName.split('/').toList
    val topicRoot = topicsList.head
     log.info(topicRoot)
    if (rootTopics.contains(topicRoot)) {
      log.info(s"Root topic $topicRoot already exist, forward to sub topic")
      rootTopics(topicRoot) ! TopicProtocol.CreateSubTopic(topicsList.tail)
    } else {
      log.info(s"Creating new root topic $topicRoot")
      val newRootTopic = context.actorOf(Props(classOf[Topic], topicRoot), topicRoot)
      rootTopics += (topicRoot -> newRootTopic)
      newRootTopic ! TopicProtocol.CreateSubTopic(topicsList.tail)
    }
  }

  def publishToTopicActor(topicName : String, message : String) = {
    lookUpTopicActor(topicName) match {
      case Some(topicRef) => topicRef ! TopicProtocol.PublishMessage( new Message(topicName, message) )
      case None           => log.info(s"trying to push to non existing topic $topicName")
    }
  }

  // FIX : find a proper way to implement this
  def lookUpTopicActor(topic : String) : Option[ActorRef] = {
    implicit val timeout = akka.util.Timeout(2 seconds)
    log.info(s"Lookup for topic actor $topic")
    val future : Future[ActorRef] = context.actorSelection(topic).resolveOne
    val topicRef = try {Await.result(future, timeout.duration).asInstanceOf[ActorRef]}
    topicRef match {
      case ref : ActorRef  => log.info(s"Lookup for topic actor $topic SUCCESS "); Some(ref)
      case _               => log.info(s"Lookup for topic actor $topic FAILED "); None  
    }
  }

  def deleteTopicActor(topicName : String) = {
    lookUpTopicActor(topicName) match {
      case Some(topicRef) => topicRef ! PoisonPill
      case None           => log.info(s"trying to delete non existing topic $topicName")
    }
  }

  def checkTopicActor(topicName : String) : Boolean = {
    lookUpTopicActor(topicName) match {
      case Some(topicRef) => true
      case None           => false
    }
  }
}

object TopicRepositoryProtocol {
  case class CreateTopicActor(topicName : String)
  case class DeleteTopicActor(topicName : String)
  case class CheckTopicActor(topicName : String)
  case class LookupTopicActor(topicName : String)
  case class PublishToTopicActor(topicName : String, message : String)
}