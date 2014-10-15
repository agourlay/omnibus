package omnibus.api.endpoint

import akka.actor._
import akka.actor.SupervisorStrategy.Stop

import scala.util._

import spray.routing._
import spray.http._
import spray.http.StatusCode
import spray.httpx.marshalling._
import spray.json._
import spray.httpx.SprayJsonSupport._

import HttpHeaders._
import DefaultJsonProtocol._

import omnibus.core.actors.CommonActor
import omnibus.api.endpoint.JsonSupport._
import omnibus.configuration._
import omnibus.api.exceptions.RequestTimeoutException
import omnibus.core.metrics.MetricsReporterProtocol._
import omnibus.domain.subscriber.SubscriberView
import omnibus.domain.subscriber.SubscriberRepositoryProtocol._
import omnibus.domain.topic.TopicRepositoryProtocol._
import omnibus.domain.topic.TopicProtocol._
import omnibus.domain.topic.TopicView
import omnibus.service.classic.RootTopics.RootTopicsSet
import omnibus.service.classic.ServiceError

class RestRequest(ctx: RequestContext, props: Props) extends CommonActor {

  implicit def system = context.system
  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(system).Timeout)

  val classicService = context.actorOf(props)

  context.setReceiveTimeout(timeout.duration)

  val timerCtx = metrics.timer(ctx.request.method.toString + "." + ctx.request.uri.path.toString).timerContext()

  def receive = {
    case ReceiveTimeout                  ⇒ requestOver(new RequestTimeoutException())
    case Failure(e)                      ⇒ requestOver(e)
    case e: Exception                    ⇒ requestOver(e)
    case ServiceError(e)                 ⇒ requestOver(e)
    // TODO add generic event handling from classic services
    case MetricsReport(metrics)          ⇒ requestOver(metrics)
    case Subscribers(subs)               ⇒ requestOver(subs)
    case sub: SubscriberView             ⇒ requestOver(sub)
    case tv: TopicView                   ⇒ requestOver(tv)
    case RootTopicsSet(roots)            ⇒ requestOver(roots)
    case MessagePublished                ⇒ requestOver(StatusCodes.Accepted, "Message published to topic\n")
    case TopicCreated(topicRef)          ⇒ requestOver(StatusCodes.Created, Location(ctx.request.uri) :: Nil, "Topic created \n")
    case TopicDeletedFromRepo(topicPath) ⇒ requestOver(StatusCodes.Accepted, "Topic deleted\n")
    case SubKilled(subId)                ⇒ requestOver(StatusCodes.Accepted, s"Subscriber $subId deleted\n")
  }

  def closeThings() {
    timerCtx.stop()
    self ! PoisonPill
  }

  def requestOver[T](payload: T)(implicit marshaller: ToResponseMarshaller[T]) = {
    ctx.complete(payload)
    closeThings()
  }

  def requestOver[T](status: StatusCode, payload: T)(implicit marshaller: ToResponseMarshaller[(StatusCode, T)]) = {
    ctx.complete(status, payload)
    closeThings()
  }

  def requestOver[T](status: StatusCode, headers: Seq[HttpHeader], payload: T)(implicit marshaller: ToResponseMarshaller[(StatusCode, Seq[HttpHeader], T)]) = {
    ctx.complete(status, headers, payload)
    closeThings
  }

  override val supervisorStrategy =
    OneForOneStrategy() {
      case e ⇒ {
        requestOver(e)
        Stop
      }
    }
}

object RestRequest {

  def props(ctx: RequestContext, props: Props) = Props(classOf[RestRequest], ctx, props).withDispatcher("requests-dispatcher")

  def perRequest(ctx: RequestContext)(props: Props)(implicit context: ActorContext) {
    context.actorOf(RestRequest.props(ctx, props))
  }
}
