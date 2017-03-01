/*
 * Copyright (C) 2017  Department for Business, Energy and Industrial Strategy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package forms.report

import org.scalatest.{EitherValues, Matchers, OptionValues, WordSpecLike}
import play.api.data.Forms._
import play.api.data.{Form, FormError}
import utils.YesNo.{No, Yes}

class ConditionalTextValidationsSpec extends WordSpecLike with Matchers with OptionValues with EitherValues {

  import ConditionalTextValidations._

  "conditionalText" should {
    val m = single("test" -> conditionalText)

    "require a yesNo answer" in {
      val result = m.bind(Map())
      val expectedErrors = List(FormError("test.yesNo", List("error.required")))
      result.left.value shouldBe expectedErrors
    }

    "result in a text of None when value is blank" in {
      val result = m.bind(Map("test.yesNo" -> "no", "test.text" -> ""))
      result.right.value shouldBe ConditionalText(No, None)
    }

    "result in a text of Non when text value is not provided" in {
      val result = Form(m).bind(Map("test.yesNo" -> "no"))
      result.value.value shouldBe ConditionalText(No, None)
    }

    "result in a text of None when value is not blank but yesno is no" in {
      val result = m.bind(Map("test.yesNo" -> "no", "test.text" -> "not blank"))
      result.right.value shouldBe ConditionalText(No, None)
    }

    "require a non-blank text when yesno is yes" in {
      val result = m.bind(Map("test.yesNo" -> "yes", "test.text" -> ""))
      val expectedErrors = List(FormError("test.text", List("error.required")))
      result.left.value shouldBe expectedErrors
    }

    "result in a valid value when yesno is yes and text is non-blank" in {
      val result = m.bind(Map("test.yesNo" -> "yes", "test.text" -> "non-blank"))
      result.right.value shouldBe ConditionalText(Yes, Some("non-blank"))
    }
  }
}

object ValidationsTestData {

  val test1 = Map(
    "filingDate" -> "2017-02-17",
    "reportDates.startDate.day" -> "1",
    "reportDates.startDate.month" -> "1",
    "reportDates.startDate.year" -> "2017",
    "reportDates.endDate.day" -> "1",
    "reportDates.endDate.month" -> "1",
    "reportDates.endDate.year" -> "2018",
    "paymentHistory.averageDaysToPay" -> "33",
    "paymentHistory.percentPaidBeyondAgreedTerms" -> "33",
    "paymentHistory.percentageSplit.percentWithin30Days" -> "33",
    "paymentHistory.percentageSplit.percentWithin60Days" -> "33",
    "paymentHistory.percentageSplit.percentBeyond60Days" -> "33",
    "paymentTerms.terms" -> "terms",
    "paymentTerms.paymentPeriod" -> "30",
    "paymentTerms.maximumContractPeriod" -> "30",
    "paymentTerms.maximumContractPeriodComment" -> "",
    "paymentTerms.paymentTermsChanged.yesNo" -> "no",
    "paymentTerms.paymentTermsChanged" -> "",
    "paymentTerms.paymentTermsNotified.yesNo" -> "no",
    "paymentTerms.paymentTermsNotified" -> "",
    "paymentTerms.paymentTermsComment" -> "",
    "disputeResolution" -> "dispute",
    "offerEInvoicing" -> "no",
    "offerSupplyChainFinancing" -> "no",
    "retentionChargesInPolicy" -> "no",
    "retentionChargesInPast" -> "no",
    "paymentCodes.yesNo" -> "no",
    "paymentCodes" -> "")
}