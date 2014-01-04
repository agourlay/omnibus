package omnibus.http.route

import akka.pattern._
import akka.actor._

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.httpx.encoding._
import spray.routing._
import spray.can.Http
import spray.can.server.Stats

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
import omnibus.domain.topic._
import omnibus.repository._
import omnibus.configuration._
import omnibus.service._
import omnibus.service.OmnibusServiceProtocol._

class StatsRoute(omnibusService: ActorRef) (implicit context: ActorContext) extends Directives {

  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(context.system).Timeout.Ask)

  val log: Logger = LoggerFactory.getLogger("omnibus.route.stat")

  val route =
    pathPrefix("stats") {
      parameters('mode.as[String] ? "simple"){ mode =>
        path("system") {
          get { ctx =>
            log.info(s"Sending server stats with $mode")
            if (mode == "streaming") {
              context.actorOf(Props(new HttpStatStream(ctx.responder)))
            } else {
              ctx.complete ((context.actorSelection("/user/IO-HTTP/listener-0") ? Http.GetStats).mapTo[Stats])
            }
          }
        } ~
        path("topics" / Rest) { topic =>
          validate(!topic.isEmpty, "topic name cannot be empty \n") {
            get { ctx =>
              log.info(s"Sending stats from topic $topic with $mode")
              if (mode == "streaming") {
                val f = (omnibusService ? OmnibusServiceProtocol.LookupTopic(topic)).mapTo[Option[ActorRef]]
                f.onComplete {
                  case Failure(result) => ctx.complete(s"Something wrong happened... \n")
                  case Success(result) => result match {
                    case Some(ref) => context.actorOf(Props(new HttpTopicStatStream(ctx.responder, ref)))
                    case None      => ctx.complete(s"topic '$topic' not found \n")
                  }
                }
              } else {
                ctx.complete ((omnibusService ? OmnibusServiceProtocol.TopicPastStat(topic)).mapTo[List[TopicStatisticState]])
              }
            }
          }
        }
      }  
    }
}