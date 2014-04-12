package omnibus.configuration

import java.io.File

trait Configuration {
	val externalConfPath = "../conf/omnibus.conf"

	if (new File(externalConfPath).exists()){
	  	System.setProperty("config.file", externalConfPath );
	}
}