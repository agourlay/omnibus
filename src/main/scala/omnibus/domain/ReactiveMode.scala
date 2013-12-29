
package omnibus.domain

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

}

import omnibus.domain.ReactiveMode._

case class ReactiveCmd(val mode: ReactiveMode, val since: Option[Long], val to: Option[Long])
case class ReactiveInput(val mode: String, val since: Option[Long], val to: Option[Long])

object ReactiveCmd {
  def apply(mode: String, since: Option[Long], to: Option[Long]): ReactiveCmd = apply(ReactiveMode.withName(mode), since, to)

  def apply(reactInput: ReactiveInput): ReactiveCmd = {
    apply(reactInput.mode, reactInput.since, reactInput.to)
  }

  //TODO use in OmnibusRest
  implicit val ReactiveCmdMarshaller =
    Unmarshaller[ReactiveCmd](ContentTypeRange.`*`) {
      case HttpEntity.NonEmpty(contentType, data) =>
        val Array(_, mode, since, to) = data.asString.split(":,".toCharArray).map(_.trim)
        val reactMode: ReactiveMode = if (mode.isEmpty) ReactiveMode.SIMPLE else ReactiveMode.withName(mode)
        val sinceOpt: Option[Long] = if (since.isEmpty) None else Some(since.toLong)
        val toOpt: Option[Long] = if (to.isEmpty) None else Some(to.toLong)
        ReactiveCmd(reactMode, sinceOpt, toOpt)
    }

}