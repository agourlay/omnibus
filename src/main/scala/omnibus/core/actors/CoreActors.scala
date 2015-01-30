package omnibus.core.actors

import omnibus.core.Core
import omnibus.core.metrics.MetricsReporter
import omnibus.domain.topic.TopicRepository
import omnibus.domain.subscriber.SubscriberRepository

trait CoreActors {
  this: Core ⇒

  val topicRepo = system.actorOf(TopicRepository.props, "topic-repository")

  val subRepo = system.actorOf(SubscriberRepository.props, "subscriber-repository")

  val metricsReporter = system.actorOf(MetricsReporter.props, "metrics-reporter")

  val unhandledlistener = system.actorOf(UnhandledMessageListener.props, "unhandled-message-listener")
}