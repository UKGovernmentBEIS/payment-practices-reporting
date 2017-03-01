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
import play.api.data.FormError

import scala.collection.mutable

class PaymentTermsChangedValidationsSpec extends WordSpecLike with Matchers with OptionValues with EitherValues {

  import PaymentTermsChangedValidations._

  val noArgs = mutable.WrappedArray.empty

  "paymentTermsChanged" should {
    "validate successfully when comment.yesNo is No" in {
      paymentTermsChanged.bind(Map("changed.yesNo" -> "no")) shouldBe a[Right[_, _]]
    }

    "validate successfully when comment.yesNo is Yes, text is supplied and notified is No" in {
      val params = Map("changed.yesNo" -> "yes", "changed.text" -> "changed", "notified.yesNo" -> "no")
      paymentTermsChanged.bind(params) shouldBe a[Right[_, _]]
    }

    "fail validation when comment.yesNo is Yes, text is supplied and notified is not present" in {
      val params = Map("changed.yesNo" -> "yes", "changed.text" -> "changed")
      val expectedError = FormError("notified.yesNo", List("error.mustanswer"), noArgs)
      paymentTermsChanged.bind(params).left.value shouldBe List(expectedError)
    }

    "fail validation when comment.yesNo is Yes, text is supplied and notified is Yes but text is not supplied" in {
      val params = Map("changed.yesNo" -> "yes", "changed.text" -> "changed", "notified.yesNo" -> "yes")
      val expectedError = FormError("notified.text", List("error.required"), noArgs)
      paymentTermsChanged.bind(params).left.value shouldBe List(expectedError)
    }

    "ignore validation of Notified when Change is No" in {
      val params = Map("changed.yesNo" -> "no", "changed.text" -> "", "notified.yesNo" -> "yes")
      paymentTermsChanged.bind(params) shouldBe a[Right[_, _]]
    }
  }
}
