package omnibus.test.domain

import omnibus.domain.topic.TopicPath
import org.scalacheck._
import Arbitrary._
import Gen._
import Prop._

object TopicPathSpecification extends Properties("TopicPath") {

	property("notEmpty") = forAll (listString) { randomList =>
	  	!TopicPath(randomList).prettyStr().isEmpty
	}

	lazy val nonEmptyStr = Gen.alphaStr suchThat ( s => s.size > 0 && s.size <= 20) 
    lazy val listString = Gen.nonEmptyContainerOf[List,String](nonEmptyStr)
}  