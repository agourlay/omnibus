package omnibus.api.endpoint

import spray.httpx.unmarshalling._

object StatisticsMode extends Enumeration {
  type StatisticsMode = Value

  val LIVE = Value("live")
  val STREAMING = Value("streaming")

  implicit val string2StatisticsMode = new FromStringDeserializer[StatisticsMode] {
    def apply(value: String): Either[DeserializationError, StatisticsMode] = {
      try Right(StatisticsMode.withName(value))
      catch {
        case e: Exception ⇒ Left(MalformedContent(s"The statisticMode is not valid - please use one of ${StatisticsMode.values}"))
      }
    }
  }
}