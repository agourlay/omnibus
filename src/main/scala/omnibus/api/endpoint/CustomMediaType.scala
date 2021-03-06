package omnibus.api.endpoint

import spray.http._
import spray.http.MediaTypes._

object CustomMediaType {
  val HALType = register(
    MediaType.custom(
      mainType = "application",
      subType = "hal+json",
      compressible = false,
      binary = false
    )
  )
}