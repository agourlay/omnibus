
package omnibus.domain.subscriber

import spray.httpx.unmarshalling._
import spray.http._

object ReactiveMode extends Enumeration {
  type ReactiveMode = Value

  val SIMPLE = Value("simple") // following events
  val LAST = Value("last") // last event and the following events
  val REPLAY = Value("replay") // all the past events and the following events
  val SINCE_ID = Value("since-id") // all the events since a given event-id  
  val SINCE_TS = Value("since-ts") // all the events since a given timestamp  
  val BETWEEN_ID = Value("between-id") // all the events between two given event-id  
  val BETWEEN_TS = Value("between-ts") // all the events between two given timestamp  

  implicit val string2ReactiveMode = new FromStringDeserializer[ReactiveMode] {
    def apply(value: String) : Either[DeserializationError, ReactiveMode] = {
      try Right(ReactiveMode.withName(value))
      catch {
        case e: Exception => Left(MalformedContent(s"The reactiveMode is not valid - please use one of ${ReactiveMode.values}"))
      }  
    }
  } 
}

import omnibus.domain.subscriber.ReactiveMode._

case class ReactiveCmd(val mode: ReactiveMode, val since: Option[Long], val to: Option[Long]){
  require( mode match {
    case SIMPLE     => true
    case LAST       => true
    case REPLAY     => true
    case SINCE_ID   => since.nonEmpty
    case SINCE_TS   => since.nonEmpty
    case BETWEEN_ID => since.nonEmpty && to.nonEmpty
    case BETWEEN_TS => since.nonEmpty && to.nonEmpty
    }, s"reactiveMode argument(s) is missing ")
}