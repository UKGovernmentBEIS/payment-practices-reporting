package forms.report

import forms.DateFields
import org.joda.time.LocalDate
import org.scalatest.{EitherValues, Matchers, WordSpecLike}
import play.api.data.FormError
import utils.SystemTimeSource

class DateFieldsSpec extends WordSpecLike with Matchers with EitherValues {
  val validations = new Validations(new SystemTimeSource)

  import validations.dateFields
  import validations.dateFromFields

  def errorFor(fieldName: String) = List(FormError(fieldName, List("error.number")))

  "dateFields" should {
    "validate successfully" in {
      dateFields.bind(Map("day" -> "1", "month" -> "10", "year" -> "2017")).right.value shouldBe DateFields(1, 10, 2017)
    }

    "reject a non-integer day" in {
      dateFields.bind(Map("day" -> "a", "month" -> "10", "year" -> "2017")).left.value shouldBe errorFor("day")
    }

    "reject a non-integer month" in {
      dateFields.bind(Map("day" -> "1", "month" -> "a", "year" -> "2017")).left.value shouldBe errorFor("month")
    }

    "reject a non-integer year" in {
      dateFields.bind(Map("day" -> "1", "month" -> "10", "year" -> "a")).left.value shouldBe errorFor("year")
    }
  }

  "dateFromFields" should {
    "validate successfully" in {
      dateFromFields.bind(Map("day" -> "1", "month" -> "10", "year" -> "2017")).right.value shouldBe new LocalDate(2017, 10, 1)
    }

    "reject an invalid set of date fields" in {
      val errors = List(FormError("", List("error.date")))

      dateFromFields.bind(Map("day" -> "1", "month" -> "13", "year" -> "2017")).left.value shouldBe errors
      dateFromFields.bind(Map("day" -> "29", "month" -> "2", "year" -> "2017")).left.value shouldBe errors
    }
  }

}
