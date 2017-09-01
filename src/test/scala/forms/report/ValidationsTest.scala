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

import config.ServiceConfig
import org.scalatest.{EitherValues, Matchers, OptionValues, WordSpecLike}
import play.api.data.FormError
import utils.SystemTimeSource

class ValidationsTest extends WordSpecLike with Matchers with OptionValues with EitherValues {
  val sut = new Validations(new SystemTimeSource, new ServiceConfig(None, None))

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

      result.left.value shouldBe List(FormError("longestPaymentPeriod", List("error.shortestNotLessThanLongest")))
    }
  }

}
