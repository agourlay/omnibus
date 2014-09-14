package omnibus.domain.subscriber

import akka.actor._

import scala.language.postfixOps

import java.security.SecureRandom
import java.math.BigInteger

import omnibus.metrics.Instrumented
import omnibus.domain.topic.TopicPath
import omnibus.domain.subscriber.SubscriberRepositoryProtocol._
import omnibus.domain.subscriber.SubscriberSupport._

class SubscriberRepository extends Actor with ActorLogging with Instrumented {

  var subs = Set.empty[SubscriberView]

  val random = new SecureRandom()

  def nextSubId = new BigInteger(130, random).toString(32)

  val subNumber = metrics.gauge("subscribers")(subs.size)
  var lookupMeter = metrics.meter("lookup")

  def receive = {
    case CreateSub(topics, responder, reactiveCmd, ip, support) => createSub(topics, responder, reactiveCmd, ip, support)
    case KillSub(id)                                            => killSub(id, sender)
    case AllSubs                                                => sender ! Subscribers(subs.toList)
    case Terminated(refSub)                                     => handleTerminated(refSub)
    case SubById(id)                                            => sender ! subLookup(id)
  }

  def subLookup(id: String) = {
    lookupMeter.mark()
    SubLookup(subs.find(_.id == id))
  }

  def createSub(topics: Set[ActorRef], responder: ActorRef, cmd: ReactiveCmd, ip: String, support: SubscriberSupport) = {
    log.info(s"Creating sub on topics $topics with support $support and channel $responder")
    val newSub = context.actorOf(Subscriber.props(responder, topics, cmd))
    val newView = SubscriberView(newSub, nextSubId, topics.map(TopicPath.prettyStr(_)).mkString("+"), ip, cmd.react.toString, support.toString)
    subs += newView
    context.watch(newSub)
  }

  def killSub(id: String, replyTo: ActorRef) = {
    subs.find(_.id == id) match {
      case None => log.info(s"Cannot delete unknown subscriber $id")
      case Some(sub) => {
        sub.ref ! PoisonPill
        subs -= (sub)
        replyTo ! SubKilled(id)
        log.info(s"Sub $sub deleted")
      }
    }
  }

  def handleTerminated(deadRef: ActorRef) = {
    subs.find(_.ref == deadRef) match {
      case Some(sub) => subs -= (sub)
      case None      => log.debug(s"Can not find dead subscriber in repository")
    }
  }
}

object SubscriberRepositoryProtocol {
  case class CreateSub(topics: Set[ActorRef], responder: ActorRef, reactiveCmd: ReactiveCmd, ip: String, support: SubscriberSupport)
  case class KillSub(id: String)
  case class SubKilled(id: String)
  case class SubById(id: String)
  case class SubLookup(opt: Option[SubscriberView])
  case class Subscribers(subs: List[SubscriberView])
  case object AllSubs
}

object SubscriberRepository {
  def props = Props(classOf[SubscriberRepository]).withDispatcher("subscribers-dispatcher")
}