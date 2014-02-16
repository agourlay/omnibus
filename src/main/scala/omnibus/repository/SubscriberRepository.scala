package omnibus.repository

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps

import omnibus.domain.subscriber.Subscriber
import omnibus.domain.subscriber.ReactiveCmd
import omnibus.domain.topic.TopicPath
import omnibus.repository.SubscriberRepositoryProtocol._
import omnibus.http.streaming.HttpTopicSubscriber

class SubscriberRepository extends Actor with ActorLogging {

  implicit def executionContext = context.dispatcher

  def receive = {
    case CreateSub(topics, responder, reactiveCmd, http) => createSub(topics, responder, reactiveCmd, http)
  }

  def createSub(topics: Set[ActorRef], responder: ActorRef, reactiveCmd: ReactiveCmd, http: Boolean) = {
    log.debug("Creating sub on topics " + topics)
    // HttpSubscriber will proxify the responder
    if (http) { 
      val prettyTopics = TopicPath.prettySubscription(topics)     
      val httpSub = context.actorOf(HttpTopicSubscriber.props(responder, reactiveCmd, prettyTopics))
      context.actorOf(Subscriber.props(httpSub, topics, reactiveCmd))
    } else context.actorOf(Subscriber.props(responder, topics, reactiveCmd))
  }

}

object SubscriberRepositoryProtocol {
  case class CreateSub(topics: Set[ActorRef], responder: ActorRef, reactiveCmd: ReactiveCmd, http: Boolean)
}

object SubscriberRepository {
	def props = Props(classOf[SubscriberRepository]).withDispatcher("subscribers-dispatcher")
}