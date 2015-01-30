package omnibus.domain.subscriber

import omnibus.domain.subscriber.ReactiveMode._

case class ReactiveCmd(react: ReactiveMode, since: Option[Long], to: Option[Long]) {
  require(react match {
    case SIMPLE     ⇒ true
    case REPLAY     ⇒ true
    case SINCE_ID   ⇒ since.nonEmpty
    case SINCE_TS   ⇒ since.nonEmpty
    case BETWEEN_ID ⇒ since.nonEmpty && to.nonEmpty
    case BETWEEN_TS ⇒ since.nonEmpty && to.nonEmpty
  }, s"reactiveMode argument(s) is missing \n")
}