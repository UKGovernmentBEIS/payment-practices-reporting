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

package models

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{EitherValues, Matchers, WordSpecLike}
import questionnaire.{YesNoAnswer, YesNoNode}
import utils.YesNo.Yes

class DecisionTreeTest extends WordSpecLike with Matchers with EitherValues with TableDrivenPropertyChecks {

  import questionnaire.DecisionTree._
  import questionnaire.Questions._

  "empty answers should return first question" in {
    val result = checkAnswers(Seq()).right.value
    result shouldBe a[YesNoNode]
    result.asInstanceOf[YesNoNode].question shouldBe isCompanyOrLLPQuestion
  }

  "wrong answer should return error" in {
    val answers = Seq(YesNoAnswer(hasSubsidiariesQuestion.id, Yes))

    val result = checkAnswers(answers)
    result shouldBe a[Left[String, _]]
    result.left.value shouldBe "Answer YesNoAnswer(3,Yes) did not match question 1"
  }

}
