package omnibus.domain.subscriber

class SubscriberNotFoundException(val subId : String) extends Exception(s"Subscriber $subId does not exist\n")