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

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{Matchers, WordSpecLike}
import utils.YesNo.{No, Yes}

class DeciderTest extends WordSpecLike with Matchers with TableDrivenPropertyChecks {

  import DeciderTestData._

  val table = Table("test records", expectedDecisions: _*)

  "check decision for each state" in {
    forAll(table) { case (state, expectedDecision) =>
      Decider.calculateDecision(state) shouldBe expectedDecision
    }
  }
}

object DeciderTestData {

  import utils.YesNo._

  val empty = DecisionState.empty
  val expectedDecisions: Seq[(DecisionState, Decision)] = Seq(
    (empty, AskQuestion(Questions.isCompanyOrLLPQuestion)),
    (empty.copy(isCompanyOrLLP = Some(No)), Exempt(None)),
    (empty.copy(isCompanyOrLLP = Some(Yes)), AskQuestion(Questions.financialYearQuestion)),
    (empty.copy(isCompanyOrLLP = Some(Yes)).copy(financialYear = Some(FinancialYear.First)), Exempt(Some("reason.firstyear")))
  ) ++
    new FYData(FinancialYear.Second, Questions.companyQuestionGroupY2, Questions.subsidiariesQuestionGroupY2).expectedDecisions ++
    new FYData(FinancialYear.ThirdOrLater, Questions.companyQuestionGroupY3, Questions.subsidiariesQuestionGroupY3).expectedDecisions 
}


class FYData(financialYear: FinancialYear, companyQuestions: ThresholdQuestions, subsidiaryQuestions: ThresholdQuestions) {
  val startState = DecisionState.empty.copy(isCompanyOrLLP = Some(Yes)).copy(financialYear = Some(financialYear))
  val Y = startState.copy(companyThresholds = Thresholds(Some(Yes)))
  val YY = Y.copy(companyThresholds = Thresholds(Some(Yes), Some(Yes)))
  val N = startState.copy(companyThresholds = Thresholds(Some(No)))
  val NY = N.copy(companyThresholds = Thresholds(Some(No), Some(Yes)))
  val NN = N.copy(companyThresholds = Thresholds(Some(No), Some(No)))
  val NYN = NY.copy(companyThresholds = Thresholds(Some(No), Some(Yes), Some(No)))
  val NYY = NY.copy(companyThresholds = Thresholds(Some(No), Some(Yes), Some(Yes)))

  val YYn = YY.copy(subsidiaries = Some(No))
  val YYy = YY.copy(subsidiaries = Some(Yes))
  val YYyY = YYy.copy(subsidiaryThresholds = Thresholds(Some(Yes)))
  val YYyYY = YYyY.copy(subsidiaryThresholds = Thresholds(Some(Yes), Some(Yes)))

  val expectedDecisions: Seq[(DecisionState, Decision)] =
    Seq(
      (startState, AskQuestion(companyQuestions.turnoverQuestion)),
      (Y, AskQuestion(companyQuestions.balanceSheetQuestion)),
      (N, AskQuestion(companyQuestions.balanceSheetQuestion)),
      (NY, AskQuestion(companyQuestions.employeesQuestion)),
      (NN, Exempt(Some("reason.company.notlargeenough"))),
      (NYN, Exempt(Some("reason.company.notlargeenough"))),

      (NYY, AskQuestion(Questions.hasSubsidiariesQuestion)),
      (YY, AskQuestion(Questions.hasSubsidiariesQuestion)),

      (YYy, AskQuestion(subsidiaryQuestions.turnoverQuestion)),
      (YYn, Required),
      (YYyY, AskQuestion(subsidiaryQuestions.balanceSheetQuestion)),
      (YYyYY, Required)
    )
}
