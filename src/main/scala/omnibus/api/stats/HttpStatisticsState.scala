package omnibus.api.stats

import spray.can.server.Stats

case class HttpStats(uptimeInMilli : Long 
   			        ,totalRequests : Long
			        ,openRequests : Long
			        ,maxOpenRequests : Long
			        ,totalConnections: Long
			        ,openConnections : Long
			        ,maxOpenConnections : Long
			        ,requestTimeouts : Long
			        ,timestamp: Long = System.currentTimeMillis / 1000)

object HttpStats {
	def fromStats(stats: Stats) = HttpStats(stats.uptime.toMillis, stats.totalRequests, stats.openRequests, stats.maxOpenRequests
		                                  , stats.totalConnections, stats.openConnections, stats.maxOpenConnections, stats.requestTimeouts) 
}