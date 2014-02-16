package omnibus.repository

import akka.actor._
import akka.pattern._

import scala.concurrent._
import scala.concurrent.Promise._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util._

import spray.caching.{ LruCache, Cache }

import omnibus.domain._
import omnibus.configuration._
import omnibus.domain.topic._
import omnibus.http.streaming.HttpTopicViewStream
import omnibus.repository.TopicRepositoryProtocol._

class TopicRepository extends Actor with ActorLogging {

  implicit def executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(context.system).Timeout.Ask)

  var rootTopics: Map[String, ActorRef] = Map.empty[String, ActorRef]

  var _seqEventId = 1L
  def nextEventId = {
    val ret = _seqEventId
    _seqEventId += 1
    ret
  }

  // cache of most looked up topicRef, conf values to be tuned
  val mostAskedTopic: Cache[Option[ActorRef]] = LruCache(maxCapacity = 100, timeToLive = 10 minute)

  def receive = {
    case CreateTopic(topic)              => checkAndCreateTopic(topic) pipeTo sender
    case DeleteTopic(topic)              => deleteTopic(topic) pipeTo sender
    case CheckTopic(topic)               => sender ! checkTopic(topic) 
    case LookupTopic(topic)              => sender ! lookUpTopicWithCache(topic)
    case PublishToTopic(topic, message)  => publishToTopic(topic, message) pipeTo sender
    case TopicPastStat(topic)            => topicPastStat(topic) pipeTo sender
    case TopicLiveStat(topic)            => topicLiveStat(topic) pipeTo sender
    case TopicViewReq(topic)             => topicView(topic) pipeTo sender
    case AllLeaves(replyTo)              => allLeaves(replyTo)
    case AllRoots                        => allRoots() pipeTo sender
    case TopicProtocol.Propagation       => log.debug("message propagation reached TopicRepository")
  }

  def checkAndCreateTopic(topicPath: TopicPath)  : Future[Boolean] = {
    val p = promise[Boolean]
    val f = p.future
    lookUpTopicWithCache(topicPath) match {
      case None           => createTopic(topicPath, p)
      case Some(topicRef) => p.failure {new TopicAlreadyExistsException(topicPath.prettyStr())}
    }
    f
  }  

  def createTopic(topicPath: TopicPath, promise : Promise[Boolean]) = {
    val topicsList = topicPath.segments
    val topicRoot = topicsList.head
    if (rootTopics.contains(topicRoot)) {
      log.debug(s"Root topic $topicRoot already exist, forward to sub topic")
      rootTopics(topicRoot) ! TopicProtocol.CreateSubTopic(topicsList.tail, promise)
    } else {
      log.debug(s"Creating new root topic $topicRoot")
      val newRootTopic = context.actorOf(Topic.props(topicRoot), topicRoot)
      rootTopics += (topicRoot -> newRootTopic)
      newRootTopic ! TopicProtocol.CreateSubTopic(topicsList.tail, promise)
    }
  }

  def publishToTopic(topicPath: TopicPath, message: String) : Future[Boolean] = {
    val p = promise[Boolean]
    val f = p.future
    lookUpTopicWithCache(topicPath) match {
      case Some(topicRef) => {
        topicRef ! TopicProtocol.PublishMessage(Message(nextEventId, topicPath, message))
        p.success(true)
       } 
      case None => {
        val topicName = topicPath.prettyStr
        log.warning(s"trying to push to non existing topic $topicName")
        p.failure {new TopicNotFoundException(topicName)}
      }
    }
    f
  }

  def lookUpTopicWithCache(topicPath: TopicPath): Option[ActorRef] = {
    val topic = topicPath.prettyStr
    log.debug(s"Lookup in cache for topic $topic")
    val futureOpt: Future[Option[ActorRef]] = mostAskedTopic(topic) { lookUpTopic(topicPath) }
    // we block here to provide an API based on Option[]
    val optResult : Option[ActorRef] = Await.result(futureOpt, timeout.duration).asInstanceOf[Option[ActorRef]]
    // do not let spray cache insert a none in the cache for the actor
    if (optResult.isEmpty) mostAskedTopic.remove(topic)
    optResult
  }

  def lookUpTopic(topicPath: TopicPath): Future[Option[ActorRef]] = {
    val topic = topicPath.prettyStr
    log.debug(s"Lookup for topic $topic")
    val future: Future[ActorRef] = context.actorSelection(topic).resolveOne
    future.map(actor => Some(actor)).recover { 
      case e: ActorNotFound => log.debug("ActorNotFound $topic during lookup"); None
      case e: Exception => log.error("Error during topic $topic lookup"); None
    }
  }

  def deleteTopic(topicPath: TopicPath) : Future[Boolean] = {
    val topicName = topicPath.prettyStr
    val p = promise[Boolean]
    val f = p.future
    log.debug(s"trying to delete topic $topicName")
    lookUpTopicWithCache(topicPath) match {
      case Some(topicRef) => {
        topicRef ! TopicProtocol.Delete
        mostAskedTopic.remove(topicName)
        if (rootTopics.contains(topicName)) rootTopics -= topicName
        p.success(true)
      }
      case None =>  p.failure { new TopicNotFoundException(topicName)}
    }
    f
  }

  def checkTopic(topicPath: TopicPath): Boolean = {
    lookUpTopicWithCache(topicPath) match {
      case Some(topicRef) => true
      case None => false
    }
  }

  def topicPastStat(topicPath: TopicPath) : Future[List[TopicStatisticValue]] = {
    val p = promise[List[TopicStatisticValue]]
    val futurResult= p.future
    val topicName = topicPath.prettyStr
    lookUpTopicWithCache(topicPath) match {
      case None => p.success(List.empty[TopicStatisticValue])
      case Some(topicRef) => p.completeWith((topicRef ? TopicStatProtocol.PastStats).mapTo[List[TopicStatisticValue]])
    }
    futurResult
  }

  def topicLiveStat(topicPath: TopicPath) : Future[TopicStatisticValue] ={
    val p = promise[TopicStatisticValue]
    val futurResult= p.future
    val topicName = topicPath.prettyStr
    lookUpTopicWithCache(topicPath) match {
      case None => p.failure { new TopicNotFoundException(topicName)}
      case Some(topicRef) => p.completeWith((topicRef ? TopicStatProtocol.LiveStats).mapTo[TopicStatisticValue])
    }
    futurResult
  }

  def topicView(topicPath: TopicPath) : Future[TopicView] = {
    val p = promise[TopicView]
    val futurResult= p.future
    val topicName = topicPath.prettyStr
    lookUpTopicWithCache(topicPath) match {
      case None => p.failure { new TopicNotFoundException(topicName)}
      case Some(topicRef) => p.completeWith((topicRef ? TopicProtocol.View).mapTo[TopicView])
    }
    futurResult
  }  

  def allLeaves(replyTo : ActorRef) {
    context.actorOf(HttpTopicViewStream.props(replyTo, rootTopics.values.toList))
  }

  def allRoots() : Future[Iterable[TopicView]] = {
    val actorTopics = rootTopics.values
    val results = for (topic <- rootTopics.values) yield (topic ? TopicProtocol.View).mapTo[TopicView]
    Future.sequence(results)
  }
}

object TopicRepositoryProtocol {
  case class CreateTopic(topicName: TopicPath)
  case class DeleteTopic(topicName: TopicPath)
  case class CheckTopic(topicName: TopicPath)
  case class LookupTopic(topicName: TopicPath)
  case class PublishToTopic(topicName: TopicPath, message: String)
  case class TopicPastStat(topic: TopicPath)
  case class TopicLiveStat(topic: TopicPath)
  case class TopicViewReq(topic: TopicPath)
  case class AllLeaves(replyTo : ActorRef)
  case object AllRoots
}

object TopicRepository {
  def props = Props(classOf[TopicRepository]).withDispatcher("topics-dispatcher")
}