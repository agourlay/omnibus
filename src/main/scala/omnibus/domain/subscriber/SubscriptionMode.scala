
package omnibus.domain.subscriber

import spray.httpx.unmarshalling._
import spray.http._

object SubscriptionMode extends Enumeration {
  type SubscriptionMode = Value

  val CLASSIC = Value("classic")
  val WIDE    = Value("wide") 

  implicit val string2ReactiveMode = new FromStringDeserializer[SubscriptionMode] {
    def apply(value: String) : Either[DeserializationError, SubscriptionMode] = {
      try Right(SubscriptionMode.withName(value))
      catch {
        case e: Exception => Left(MalformedContent(s"The subscriptionMode is not valid - please use one of ${SubscriptionMode.values} \n"))
      }  
    }
  } 
}