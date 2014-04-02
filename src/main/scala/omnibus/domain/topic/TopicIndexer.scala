package omnibus.domain.topic

import akka.actor._

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mapping.FieldType._

import omnibus.configuration._
import omnibus.domain.message._

class TopicIndexer extends Actor with ActorLogging{

	val system = context.system

	val indexerHost = Settings(system).Indexer.Host

    val client = ElasticClient.remote(indexerHost, 9300)

	system.eventStream.subscribe(self, classOf[Message])

	log.info(s"Starting topicIndexer to $indexerHost:9200")

	def receive = {
		case m : Message => indexMessage(m)
	}

	def indexMessage(m : Message) = {
		client.execute {
    		index into m.topicPath.prettyStr() id m.id fields (
		        "payload" -> m.payload
		  	)
		}
	}
}

object TopicIndexer {  
  def props() = Props(classOf[TopicIndexer]).withDispatcher("topics-dispatcher")
}