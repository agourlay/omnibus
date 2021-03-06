package omnibus.domain.topic

import akka.actor.ActorRef

case class TopicPathRef(topicPath: TopicPath, topicRef: Option[ActorRef])

object TopicPathRef {
  def apply(ref: ActorRef): TopicPathRef = TopicPathRef(TopicPath(ref), Some(ref))
}

case class TopicPath(segments: List[String]) {
  require(segments.nonEmpty, s"Topic path is empty \n")
  require(segments.size < 50, s"Topic path is too long \n")
  require(segments.exists(_.length < 20), s"Topic path contains overlong segment \n")
  require(!segments.exists(_.isEmpty), s"Topic path contains an empty segment \n")
  require(!segments.forall(TopicPath.containsBadChars), s"Topic path contains forbidden chars \n")

  def prettyStr = segments.mkString("/")
}

object TopicPath {
  def apply(rawPath: String): TopicPath = TopicPath(rawPath.split('/').toList)

  def apply(ref: ActorRef): TopicPath = {
    val refPath = TopicPath.prettyStr(ref)
    TopicPath(refPath)
  }

  def multi(rawPaths: String): List[TopicPath] = rawPaths.split("[/]\\+[/]")
    .toList
    .map(TopicPath(_))

  def splitMultiTopic(topics: String): List[String] = topics.split("[/]\\+[/]").toList

  // TODO : find a way to remove the hardcoded string
  def prettyStr(ref: ActorRef) = ref.path.toString.split("/topic-repository/").toList(1)

  def prettySubscription(topics: Set[ActorRef]): String = {
    val setOfTopic = topics.map(TopicPath.prettyStr)
    setOfTopic.mkString(" + ")
  }

  // TODO : dumb list, replace it with a cryptic regex
  val forbiddenChars: List[String] = List("*", " ", "?", "%", "(", ")")

  def containsBadChars(segment: String): Boolean = forbiddenChars.exists(segment.contains(_))
}