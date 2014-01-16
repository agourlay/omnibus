
package omnibus.domain.topic

import spray.httpx.unmarshalling._
import spray.http._

object StatisticsMode extends Enumeration {
  type StatisticsMode = Value

  val LIVE = Value("live") 
  val HISTORY = Value("history") 
  val STREAMING = Value("streaming") 

  implicit val string2StatisticsMode = new FromStringDeserializer[StatisticsMode] {
    def apply(value: String) : Either[DeserializationError, StatisticsMode] = {
      try Right(StatisticsMode.withName(value))
      catch {
        case e: Exception => Left(MalformedContent(s"The statisticMode is not valid - please use one of ${StatisticsMode.values}"))
      }  
    }
  } 
}