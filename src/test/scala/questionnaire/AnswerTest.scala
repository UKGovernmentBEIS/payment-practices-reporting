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

package questionnaire

import org.scalatest.{Matchers, WordSpecLike}
import play.api.libs.json.{JsSuccess, Json}
import questionnaire.FinancialYear._
import utils.YesNo.{No, Yes}

class AnswerTest extends WordSpecLike with Matchers {

  "round trip" should {

    "preserve a YesNoAnswer" in {
      val yn = YesNoAnswer(1, No)
      val o = Answer.ynFormat.writes(yn)
      val actual = Answer.ynFormat.reads(o)

      actual shouldBe a[JsSuccess[_]]
      actual.asInstanceOf[JsSuccess[YesNoAnswer]].value shouldBe yn
    }

    "preserve a FinancialYearAnswer" in {
      val fy = FinancialYearAnswer(1, First)
      val o = Answer.fyFormat.writes(fy)
      val actual = Answer.fyFormat.reads(o)

      actual shouldBe a[JsSuccess[_]]
      actual.asInstanceOf[JsSuccess[FinancialYearAnswer]].value shouldBe fy
    }

    "preserve values" in {
      val answers = Seq(
        YesNoAnswer(5, No),
        YesNoAnswer(7, Yes),
        FinancialYearAnswer(7, First),
        FinancialYearAnswer(8, Second),
        FinancialYearAnswer(10, ThirdOrLater)
      )

      val encoded = Json.toJson(answers)

      val decoded = Json.fromJson[Seq[Answer]](encoded)
      decoded shouldBe a[JsSuccess[_]]
      decoded.asInstanceOf[JsSuccess[Seq[Answer]]].value shouldBe answers

    }
  }

}
