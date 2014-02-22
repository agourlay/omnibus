package omnibus.domain.topic

import akka.actor._

import omnibus.domain._

case class TopicPath(segments : List[String]) {
	require(segments.size != 0 , s"Topic path is empty \n")
	require(segments.size < 50 , s"Topic path is too long \n")
	require(segments.exists(_.size < 20) , s"Topic path contains overlong segment \n")
	require(!segments.exists(_.isEmpty) , s"Topic path contains an empty segment \n")
	require(segments.map(TopicPath.containsBadChars(_)).exists(_ == false) , s"Topic path contains forbidden chars \n")

	def prettyStr() = segments.mkString("/")
}

object TopicPath {
	def apply(rawPath : String) : TopicPath = TopicPath(rawPath.split('/').toList)

	def apply(ref : ActorRef) : TopicPath = {
		val refPath = TopicPath.prettyStr(ref)
		TopicPath(refPath)
	}

	def multi(rawPaths : String) : List[TopicPath] = rawPaths.split("[/]\\+[/]")
	                                                         .toList
	                                                         .map(TopicPath(_))

	def splitMultiTopic(topics: String): List[String] = topics.split("[/]\\+[/]").toList

	def prettyStr(ref : ActorRef) = ref.path.toString.split("/topic-repository/").toList(1)

	def prettySubscription(topics: Set[ActorRef]): String = {
	    val setOfTopic = topics.map(TopicPath.prettyStr(_))
	    setOfTopic.mkString(" + ")
    }

	// TODO : dumb list, replace it with a cryptic regex
	val forbiddenChars:List[String] = List("*"
										  ," "	
		 								  ,"?"
			  						      ,"%"
			  						      ,"("
			  						  	  ,")")

	def containsBadChars(segment : String) : Boolean = forbiddenChars.exists(segment.contains(_))
}