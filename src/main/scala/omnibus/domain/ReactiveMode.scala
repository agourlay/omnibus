
package omnibus.domain

object ReactiveMode extends Enumeration {
  type ReactiveMode = Value
  val SIMPLE = Value("simple")
  val LAST   = Value("last")
  val REPLAY = Value("replay")
  val SINCE  = Value("since") 
  val NEXT   = Value("next")
}