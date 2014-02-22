package omnibus.service

import akka.actor._
import akka.actor.ActorLogging
import akka.pattern._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

import omnibus.service.OmnibusServiceProtocol._
import omnibus.repository._
import omnibus.configuration._
import omnibus.domain._
import omnibus.domain.topic._
import omnibus.domain.subscriber._

import reflect.ClassTag

class OmnibusService(topicRepo: ActorRef, subscriberRepo: ActorRef) extends Actor with ActorLogging {
  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(context.system).Timeout.Ask)

  def receive = {
    case SubToTopic(topic, responder, reactiveCmd, http) => subToTopic(topic, responder, reactiveCmd, http) pipeTo sender
  }

  def lookupTopic(topicPath: TopicPath) : Future[Option[ActorRef]] = {
    (topicRepo ? TopicRepositoryProtocol.LookupTopic(topicPath)).mapTo[Option[ActorRef]]
  }

  def subToTopic(topicPath: TopicPath, responder: ActorRef, reactiveCmd: ReactiveCmd, ip: String): Future[Boolean] = {
    val topicName = topicPath.prettyStr
    log.debug(s"Request to subscribe to $topicName with reactive cmd $reactiveCmd")
    val p = promise[Boolean]
    val futurResult: Future[Boolean] = p.future

    // Get all future topic ActorRef to subscribe to
    val actorTopics = for (topic <- TopicPath.splitMultiTopic(topicName)) yield lookupTopic(topicPath)

    //List[Future[Option]] to Future[List[Option]]                 
    Future.sequence(actorTopics).onComplete {
      case Failure(error) => {
        log.warning("an error occured while subscribing to topic " + error.getMessage())
        p.failure { new Exception("an error occured while subscribing to topic ") }
      }
      case Success(optTopicRefList) => {
        // All topics must exist in case of a composed subscriptions
        if (optTopicRefList.forall(_.nonEmpty)) {
          val topicRefList: List[ActorRef] = optTopicRefList.filter(_.nonEmpty).map(_.get)
          subscriberRepo ! SubscriberRepositoryProtocol.CreateSub(topicRefList.toSet, responder, reactiveCmd, ip)
          p.success(true)
        } else {
          log.info("None of the topics exist")
          p.failure { new TopicNotFoundException(topicName) }
        }
      }
    }
    futurResult
  }
}

object OmnibusServiceProtocol {
  case class SubToTopic(topic: TopicPath, subscriber: ActorRef, reactiveCmd: ReactiveCmd, ip: String)
}

object OmnibusService {
  def props(topicRepo :ActorRef, subRepo :ActorRef) = Props(classOf[OmnibusService], topicRepo, subRepo)
}