package omnibus.repository

import akka.actor._

import scala.language.postfixOps

import java.security.SecureRandom
import java.math.BigInteger

import omnibus.domain.subscriber._
import omnibus.domain.subscriber.Subscriber
import omnibus.domain.subscriber.ReactiveCmd
import omnibus.domain.subscriber.SubscriberView
import omnibus.domain.topic.TopicPath
import omnibus.repository.SubscriberRepositoryProtocol._
import omnibus.http.streaming.HttpTopicSubscriber

class SubscriberRepository extends Actor with ActorLogging {

  implicit def executionContext = context.dispatcher

  var subs: Set[SubscriberView] = Set.empty[SubscriberView]  

  val random = new SecureRandom()

  def nextSubId = new BigInteger(130, random).toString(32)

  def receive = {
    case CreateSub(topics, responder, reactiveCmd, http) => createSub(topics, responder, reactiveCmd, http)
    case KillSub(id)                                     => killSub(id, sender)
    case AllSubs                                         => sender ! subs.toList
    case Terminated(refSub)                              => handleTerminated(refSub)
    case SubById(id)                                     => sender ! SubLookup(subs.find(_.id == id))
  }

  def createSub(topics: Set[ActorRef], responder: ActorRef, reactiveCmd: ReactiveCmd, ip: String) = {
    log.debug("Creating sub on topics " + topics)
    // HttpSubscriber will proxify the responder
    val prettyTopics = TopicPath.prettySubscription(topics)     
    val httpSub = context.actorOf(HttpTopicSubscriber.props(responder, reactiveCmd, prettyTopics))
    val newSub = context.actorOf(Subscriber.props(httpSub, topics, reactiveCmd))
    subs += (SubscriberView(newSub, nextSubId, topics.map(TopicPath.prettyStr(_)).mkString("+"), ip))
    context.watch(newSub)
  }

  def killSub(id : String, replyTo : ActorRef) = {
    subs.find(_.id == id) match {
      case None => log.info(s"Cannot delete unknown subscriber $id")
      case Some (sub) =>  {
        sub.ref ! PoisonPill
        subs -= (sub)
        replyTo ! SubKilled(id)
      }  
    }
  }

  def handleTerminated(deadRef : ActorRef) = {
    subs.find(_.ref == deadRef) match {
      case Some (sub) =>  subs -= (sub)
      case None => log.debug(s"Can not find dead subscriber in repository")      
    }
  }
}

object SubscriberRepositoryProtocol {
  case class CreateSub(topics: Set[ActorRef], responder: ActorRef, reactiveCmd: ReactiveCmd, ip: String)
  case class KillSub(id : String)
  case class SubKilled(id : String)
  case class SubById(id : String)
  case class SubLookup(opt : Option[SubscriberView])
  case object AllSubs
}

object SubscriberRepository {
	def props = Props(classOf[SubscriberRepository]).withDispatcher("subscribers-dispatcher")
}