package omnibus.core

import omnibus.configuration._
import omnibus.domain.topic.TopicIndexer
import omnibus.domain.topic.TopicRepository
import omnibus.domain.subscriber.SubscriberRepository
import omnibus.api.stats._

trait CoreActors {
  this: Core =>

  val topicRepo = system.actorOf(TopicRepository.props, "topic-repository")

  val subRepo = system.actorOf(SubscriberRepository.props, "subscriber-repository")

  val httpStatService = system.actorOf(HttpStatistics.props, "http-stat-service")

  if (Settings(system).Indexer.Enable) system.actorOf(TopicIndexer.props, "indexer-service")

}