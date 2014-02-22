package omnibus.http.request

import akka.pattern._
import akka.actor._

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.httpx.encoding._
import spray.routing._
import spray.routing.authentication._
import spray.json._
import spray.httpx.marshalling._
import spray.http._
import HttpHeaders._
import MediaTypes._

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util._

import DefaultJsonProtocol._
import reflect.ClassTag

import omnibus.http.JsonSupport._
import omnibus.http.streaming._
import omnibus.configuration._
import omnibus.domain._
import omnibus.domain.topic._
import omnibus.domain.subscriber._
import omnibus.repository._
import omnibus.http.request.SubToTopicRequestProtocol._

class SubToTopicRequest(topicPath: TopicPath, reactiveCmd: ReactiveCmd, ip: String, ctx : RequestContext
                      , subRepo : ActorRef, topicRepo: ActorRef) extends Actor with ActorLogging {

  implicit def executionContext = context.dispatcher
  implicit def system = context.system
  implicit val timeout = akka.util.Timeout(Settings(system).Timeout.Ask)

  var pending: Set[TopicPath] = Set.empty[TopicPath] 
  var ack: Set[ActorRef] = Set.empty[ActorRef]

  override def preStart() = {
    system.scheduler.scheduleOnce(timeout.duration, self, SubToTopicRequestProtocol.RequestTimeout)
    val topics = TopicPath.multi(topicPath.prettyStr)
    topics foreach { topic =>
      pending += topic
      topicRepo ! TopicRepositoryProtocol.LookupTopic(topic)
    }
  }  
 
  def handleTimeout : Receive = {
    case RequestTimeout => {
      ctx.complete(new AskTimeoutException("Subscription timeout"))
      self ! PoisonPill
    }  
  }

  def receive = ({
    case tpr : TopicPathRef => handleTopicPathRef(tpr)
  }: Receive) orElse handleTimeout

  def handleTopicPathRef(tpr : TopicPathRef) = tpr.topicRef match {
    case None           => {
      ctx.complete(new TopicNotFoundException(tpr.topicPath.prettyStr))
      self ! PoisonPill
    }  
    case Some(topicRef) => {
      ack += topicRef
      if (ack.size == pending.size) {
        subRepo ! SubscriberRepositoryProtocol.CreateSub(ack, ctx.responder, reactiveCmd, ip)
        self ! PoisonPill
      }
    }  
  }
}

object SubToTopicRequest {
   def props(topicPath: TopicPath, reactiveCmd: ReactiveCmd, ip: String, ctx : RequestContext, subRepo : ActorRef, topicRepo: ActorRef) 
     = Props(classOf[SubToTopicRequest], topicPath, reactiveCmd, ip, ctx, subRepo , topicRepo).withDispatcher("subscribers-dispatcher")
}

object SubToTopicRequestProtocol {
  case object RequestTimeout
}