package omnibus.domain.topic

import akka.actor._
import akka.pattern._
import akka.persistence._

import scala.concurrent.Future
import scala.language.postfixOps

import omnibus.core.actors.CommonActor
import omnibus.configuration.Settings
import omnibus.domain.topic.TopicRepositoryProtocol._

class TopicRepository extends PersistentActor with CommonActor {

  implicit val executionContext = context.dispatcher
  implicit val timeout = akka.util.Timeout(Settings(context.system).Timeout)

  override def persistenceId = self.path.toStringWithoutAddress

  val rootTopics = scala.collection.mutable.Map.empty[String, ActorRef]

  val rootTopicsNumber = metrics.gauge("root-topics")(rootTopics.size)
  val topicsNumber = metrics.counter("topics")

  var state = TopicRepoState()
  def updateState(msg: TopicRepoStateValue) { state = state.update(msg) }

  val receiveRecover: Receive = {
    case t: TopicRepoStateValue ⇒
      createTopic(t.topicPath, context.system.deadLetters) //no need for ACK
      updateState(TopicRepoStateValue(t.seqNumber, t.topicPath))
    case SnapshotOffer(_, snapshot: TopicRepoState) ⇒
      state = snapshot
      state.events foreach { t ⇒ createTopic(t.topicPath, context.system.deadLetters) } //no need for ACK
  }

  val receiveCommand: Receive = {
    case CreateTopic(topic)                 ⇒ persistTopic(topic, sender())
    case DeleteTopic(topic)                 ⇒ sender ! deleteTopic(topic)
    case AllRoots                           ⇒ sender ! Roots(rootTopics.values.toVector.map(TopicPathRef(_)))
    case LookupTopic(topic)                 ⇒ lookUpTopic(topic) pipeTo sender()
    case TopicProtocol.Propagation(op, dir) ⇒ log.debug("message propagation reached Repo")
  }

  def persistTopic(topicPath: TopicPath, replyTo: ActorRef) = {
    persistAsync(TopicRepoStateValue(lastSequenceNr + 1, topicPath)) { t ⇒
      createTopic(t.topicPath, replyTo)
      updateState(TopicRepoStateValue(lastSequenceNr, topicPath))
    }
  }

  def createTopic(topicPath: TopicPath, replyTo: ActorRef) = {
    val topicsList = topicPath.segments
    val topicRoot = topicsList.head
    topicsNumber += 1
    if (rootTopics.contains(topicRoot)) {
      log.debug(s"Root topic $topicRoot already exist, forward to sub topic")
      rootTopics(topicRoot) ! TopicProtocol.CreateSubTopic(topicsList.tail, replyTo)
    } else {
      log.debug(s"Creating new root topic $topicRoot")
      val newRootTopic = context.actorOf(Topic.props(topicRoot), topicRoot)
      rootTopics += (topicRoot -> newRootTopic)
      newRootTopic ! TopicProtocol.CreateSubTopic(topicsList.tail, replyTo)
    }
  }

  // TODO clean the crazy Future mapping
  def lookUpTopic(topicPath: TopicPath): Future[TopicPathRef] = timing("lookup") {
    val futureOpt: Future[ActorRef] = context.actorSelection(topicPath.prettyStr).resolveOne
    futureOpt.map { topicRef ⇒ Some(topicRef) }
      .recover { case e: Exception ⇒ log.debug(s"$e"); None }
      .map { optTopicRef ⇒ TopicPathRef(topicPath, optTopicRef) }
  }

  def deleteTopic(topicPath: TopicPath) = {
    val topicName = topicPath.prettyStr
    log.debug(s"trying to delete topic $topicName")
    if (rootTopics.contains(topicName)) rootTopics -= topicName
    state.events.find(_.topicPath == topicPath) match {
      case None ⇒ log.info(s"Cannot find topic in repo state")
      case Some(topic) ⇒
        topicsNumber -= 1
        // TODO : should not delete message if it is valid
        deleteMessage(topic.seqNumber, permanent = true)
        state = TopicRepoState(state.events.filterNot(_.topicPath == topicPath))
    }
    TopicDeletedFromRepo(topicPath)
  }
}

object TopicRepositoryProtocol {
  case class CreateTopic(topicName: TopicPath)
  case class DeleteTopic(topicName: TopicPath)
  case class LookupTopic(topicName: TopicPath)
  case class TopicDeletedFromRepo(topicName: TopicPath)
  case class Roots(refs: Vector[TopicPathRef])
  case object AllRoots
}

object TopicRepository {
  def props = Props(classOf[TopicRepository]).withDispatcher("topics-dispatcher")
}