package omnibus.http

import spray.http._
import spray.http.MediaTypes._
import HttpHeaders._

object CustomMediaType {
	val HALType = register(
	    MediaType.custom(
	    	mainType = "application",
	    	subType = "hal+json",
	    	compressible = false,
	    	binary = false
	    )
    )

    val EventStreamType = register(
	    MediaType.custom(
	    	mainType = "text",
	    	subType = "event-stream",
	    	compressible = false,
	    	binary = false
	    )
	)
}