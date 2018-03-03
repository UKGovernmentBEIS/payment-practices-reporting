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

package forms

import forms.Validations.words
import org.scalatest.{EitherValues, Matchers, WordSpecLike}
import play.api.data.Forms._

class Validations$Test extends WordSpecLike with Matchers with EitherValues {

  "words validation" should {
    val m = single("test" -> words(maxWords = 5))

    "be successful when word limit not exceeded" in {
      val s = "only three words"
      val result = m.bind(Map("test" -> s))

      result.right.value shouldBe s
    }

    "fail when word limit is exceeded" in {
      val s = "here we have six little words"
      val result = m.bind(Map("test" -> s))

      result shouldBe a[Left[_, _]]
      val errors = result.left.value
      errors.length shouldBe 1
      errors.head.message shouldBe "error.maxWords"
    }

    "fail when the words count is okay but overall string is too long" in {
      // average word length is taken to be 7, so five words should not exceed 35 characters, including whitespace
      val s = "herefore wordcount numbering five butverylongones"
      val result = m.bind(Map("test" -> s))

      result shouldBe a[Left[_, _]]
      val errors = result.left.value
      errors.length shouldBe 1
      errors.head.message shouldBe "error.maxLength"
    }
  }

}
