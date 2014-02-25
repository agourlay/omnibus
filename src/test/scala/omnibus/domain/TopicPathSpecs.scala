package omnibus.test.domain

import omnibus.domain.topic.TopicPath
import org.scalacheck._
import Gen._
import Prop._

object TopicPathSpecification extends Properties("TopicPath") {

	val notTooLong = Gen.identifier filter (_.size <= 20)
    implicit val listString = Gen.nonEmptyContainerOf[List,String](notTooLong)

	property("notEmpty") = forAll(listString) { randomList =>
	  	!TopicPath(randomList).prettyStr().isEmpty
	}
}  