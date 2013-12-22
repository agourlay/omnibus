
package omnibus.domain

object ReactiveMode extends Enumeration {
  type ReactiveMode = Value
  val SIMPLE, LAST, REPLAY, SINCE, NEXT = Value
}