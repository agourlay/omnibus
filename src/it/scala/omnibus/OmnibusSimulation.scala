package omnibus.it

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import java.io.File

import org.apache.commons.io.FileUtils

import omnibus._

trait OmnibusSimulation extends Simulation {

	val minSuccessPercentage = 95
	val maxResponseTimePercentile1 = 500
	
	// TODO access value via config
	val httpBasicUser = "admin"	
	val httpBasicPwd = "omnibus"
	val leveldbDir = "../data/journal"
	val snapshotsDir = "../data/snapshots"

	val storageLocations = List(leveldbDir, snapshotsDir).map(s ⇒ new File(s))

	before {
	  storageLocations.foreach(FileUtils.deleteDirectory)
	  omnibus.Boot
	}

	after {
		storageLocations.foreach(FileUtils.deleteDirectory)
	}
}