package omnibus.test.domain

import scala.util.Random

import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpecLike
import org.scalatest.Matchers

import com.typesafe.config.ConfigFactory

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.DefaultTimeout
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import scala.concurrent.duration._
import scala.collection.immutable

import omnibus.domain.topic.Topic
import omnibus.domain.topic.Topic._
import omnibus.domain.topic._
import omnibus.domain.topic.TopicProtocol._

class TestKitTopicSpec extends TestKit(ActorSystem("TestKitTopicSpec", ConfigFactory.parseString(TestKitTopicSpec.config)))
    with DefaultTimeout with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  import TestKitTopicSpec._

  val topicRepo = system.actorOf(RepoActor.props(self), "topic-repository")

  "A publish message" should {
    "Response with the ack" in {
      within(5 seconds) {
        topicRepo ! TopicProtocol.PublishMessage("Nananananana")
        expectMsg(TopicProtocol.MessagePublished)
      }
    }
  }
  override def afterAll {
    shutdown()
  }
}

object TestKitTopicSpec {
  // Define your test specific configuration here
  val config = """
    
akka {
    loggers = ["akka.event.slf4j.Slf4jLogger"]
    log-config-on-start = off
    log-dead-letters = off
    loglevel = "INFO"
    executor = "fork-join-executor"
    fork-join-executor {
        parallelism-min = 1
        parallelism-factor = 1.0
        parallelism-max = 4
    }
    throughput = 20
    persistence {
        journal {
            plugin = "cassandra-journal"
            max-message-batch-size = 500
        }
    }
    debug {
        receive = on
        autoreceive = on
        lifecycle = on
    }
}
    omnibus {
    http {
        port = 8080
    }
    admin {
        userName = "admin"
        password = "omnibus"
    }
    topic {
        retentionTime = "3 days"
    }
    statistics{
        storageInterval = "60 seconds"
        retentionTime = "3 days"
    }
}

topics-dispatcher {
    type = Dispatcher
    executor = "fork-join-executor"
    fork-join-executor {
        parallelism-min = 1
        parallelism-factor = 1.0
        parallelism-max = 1
    }
    throughput = 20
}

akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
    """

  class RepoActor(senderReply: ActorRef) extends Actor {
    import omnibus.domain.topic.TopicProtocol._
    val topicRef = context.actorOf(Topic.props("batman"), "batman")

    def receive = {
      case msg: TopicProtocol.PublishMessage    ⇒ topicRef ! msg
      case msg @ TopicProtocol.MessagePublished ⇒ senderReply ! msg
    }
  }

  object RepoActor {
    def props(senderReply: ActorRef) = Props(classOf[RepoActor], senderReply)
  }

}