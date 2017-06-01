package forms.report

import config.ServiceConfig
import org.scalatest.{EitherValues, Matchers, OptionValues, WordSpecLike}
import play.api.data.FormError
import utils.SystemTimeSource

class ValidationsTest extends WordSpecLike with Matchers with OptionValues with EitherValues {
  val sut = new Validations(new SystemTimeSource, new ServiceConfig(None))

  "paymentTerms" should {
    "raise an error if the shortest payment period is not less than the longest payment period" in {
      val result: Either[Seq[FormError], PaymentTerms] = sut.paymentTerms.bind(Map[String, String](
        "shortestPaymentPeriod" -> "5",
        "longestPaymentPeriod" -> "3",
        "terms" -> "terms",
        "maximumContractPeriod" -> "30",
        "paymentTermsChanged.changed.yesNo" -> "no",
        "disputeResolution" -> "disputeResolution"
      ))

      result.left.value shouldBe List(FormError("paymentTerms.longestPaymentPeriod", List("error.shortestNotLessThanLongest")))
    }
  }

}
