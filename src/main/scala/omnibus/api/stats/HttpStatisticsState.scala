package omnibus.api.stats

import spray.can.server.Stats

case class HttpStatisticsState(events: List[HttpStats] = Nil) {
  def update(msg: HttpStats) = copy(msg :: events)
  def size = events.length
  override def toString: String = events.reverse.toString
}

case class HttpStats(seqNumber : Long
	                ,uptimeInMilli : Long 
   			        ,totalRequests : Long
			        ,openRequests : Long
			        ,maxOpenRequests : Long
			        ,totalConnections: Long
			        ,openConnections : Long
			        ,maxOpenConnections : Long
			        ,requestTimeouts : Long
			        ,timestamp: Long = System.currentTimeMillis / 1000)

object HttpStats {
	def fromStats(seqNumber : Long, stats: Stats) = HttpStats(seqNumber, stats.uptime.toMillis
		    ,stats.totalRequests ,stats.openRequests, stats.maxOpenRequests, stats.totalConnections
			,stats.openConnections, stats.maxOpenConnections, stats.requestTimeouts) 
}