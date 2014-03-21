package omnibus.core

import akka.actor._

import omnibus.api._
import omnibus.repository._
import omnibus.configuration._
import omnibus.api.stats._

trait CoreActors {
  this: Core =>

  val topicRepo = system.actorOf(TopicRepository.props, "topic-repository")

  val subRepo = system.actorOf(SubscriberRepository.props, "subscriber-repository")

  val httpStatService = system.actorOf(HttpStatistics.props, "http-stat-service")

}