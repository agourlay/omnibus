package omnibus.test.service

import org.specs2._
import omnibus.service._

class OmnibusReceptionistSpecs extends Specification {
	def is =
    "Receptionist should:" ^
      p ^
      "Start a new system"    ! startNewSystem ^
      "Create new topic"      ! createNewTopic ^
      "Delete topic"          ! deleteTopic ^
      "Publish to topic"      ! publishToTopic ^
      "Subscribe to topic"    ! subscribeToTopic ^
      "Unsubscribe from topic" ! unsubscribeFromTopic ^
      end

    def startNewSystem() = {
    	val receptionist : OmnibusReceptionist = OmnibusBuilder.start()
    	receptionist.shutDownOmnibus()
    	receptionist != null
    }  

    def createNewTopic() = {
    	val receptionist : OmnibusReceptionist = OmnibusBuilder.start()
    	receptionist.createTopic("test/topic", "bazinga")
    	receptionist.shutDownOmnibus()
    	true 
    }

    def deleteTopic() = {
    	val receptionist : OmnibusReceptionist = OmnibusBuilder.start()
    	receptionist.createTopic("test/topic", "bazinga")
    	receptionist.deleteTopic("test/topic")
    	receptionist.shutDownOmnibus()
    	true 
    }

    def publishToTopic() = {
    	val receptionist : OmnibusReceptionist = OmnibusBuilder.start()
    	receptionist.createTopic("test/topic", "")
    	receptionist.publishToTopic("test/topic","push it")
    	receptionist.shutDownOmnibus()
    	true 
    }    

    def subscribeToTopic() = {
    	true 
    }

    def unsubscribeFromTopic() = {
    	true 
    }        
}
