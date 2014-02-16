package omnibus.domain.topic

class TopicNotFoundException(val topicName : String) extends Exception(s"Topic $topicName does not exist\n")

class TopicAlreadyExistsException(val topicName : String) extends Exception(s"Topic $topicName alread exists\n")