
package omnibus.domain.subscriber

import spray.httpx.unmarshalling._
import spray.http._

object ReactiveMode extends Enumeration {
  type ReactiveMode = Value

  val SIMPLE = Value("simple") // following events
  val REPLAY = Value("replay") // all the past events and the following events
  val SINCE_ID = Value("since-id") // all the events since a given event-id  
  val SINCE_TS = Value("since-ts") // all the events since a given timestamp  
  val BETWEEN_ID = Value("between-id") // all the events between two given event-id  
  val BETWEEN_TS = Value("between-ts") // all the events between two given timestamp  

  implicit val string2ReactiveMode = new FromStringDeserializer[ReactiveMode] {
    def apply(value: String) : Either[DeserializationError, ReactiveMode] = {
      try Right(ReactiveMode.withName(value))
      catch {
        case e: Exception => Left(MalformedContent(s"The reactiveMode is not valid - please use one of ${ReactiveMode.values} \n"))
      }  
    }
  } 
}