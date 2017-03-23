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

import org.joda.time.LocalDate
import org.scalatest.{EitherValues, Matchers, WordSpecLike}
import play.api.data.FormError

class DateFieldsSpec extends WordSpecLike with Matchers with EitherValues {
  import Validations.{dateFields, dateFromFields}

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

    "reject invalid date fields" in {
      val errors = List(FormError("", List("error.date")))

      dateFromFields.bind(Map("day" -> "", "month" -> "13", "year" -> "2017")).left.value shouldBe errors
    }

    "reject an set of numeric date fields that do not represent a valid date" in {
      val errors = List(FormError("", List("error.date")))

      dateFromFields.bind(Map("day" -> "1", "month" -> "13", "year" -> "2017")).left.value shouldBe errors
      dateFromFields.bind(Map("day" -> "29", "month" -> "2", "year" -> "2017")).left.value shouldBe errors
    }
  }

}
