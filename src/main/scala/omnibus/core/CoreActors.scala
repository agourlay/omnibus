package omnibus.core

import omnibus.domain.topic.TopicRepository
import omnibus.domain.subscriber.SubscriberRepository
import omnibus.metrics.MetricsReporter

trait CoreActors {
  this: Core =>

  val topicRepo = system.actorOf(TopicRepository.props, "topic-repository")

  val subRepo = system.actorOf(SubscriberRepository.props, "subscriber-repository")

  val metricsReporter = system.actorOf(MetricsReporter.props, "metrics-reporter")
}