package omnibus.domain.message

object PropagationDirection extends Enumeration {
  type PropagationDirection = Value

  val UP = Value("up")
  val DOWN = Value("down")
}