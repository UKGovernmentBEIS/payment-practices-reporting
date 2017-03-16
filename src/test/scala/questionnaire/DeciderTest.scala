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
  val s0 = empty.copy(isCompanyOrLLP = Some(No))
  val s1 = empty.copy(isCompanyOrLLP = Some(Yes))

  val fy1 = s1.copy(financialYear = Some(FinancialYear.First))

  val fy2 = s1.copy(financialYear = Some(FinancialYear.Second))
  val fy2Y = fy2.copy(companyThresholds = Thresholds(Some(Yes)))
  val fy2YY = fy2Y.copy(companyThresholds = Thresholds(Some(Yes), Some(Yes)))
  val fy2N = fy2.copy(companyThresholds = Thresholds(Some(No)))
  val fy2NY = fy2N.copy(companyThresholds = Thresholds(Some(No), Some(Yes)))
  val fy2NN = fy2N.copy(companyThresholds = Thresholds(Some(No), Some(No)))
  val fy2NYN = fy2NY.copy(companyThresholds = Thresholds(Some(No), Some(Yes), Some(No)))
  val fy2NYY = fy2NY.copy(companyThresholds = Thresholds(Some(No), Some(Yes), Some(Yes)))

  val fy2YYn = fy2YY.copy(subsidiaries = Some(No))
  val fy2YYy = fy2YY.copy(subsidiaries = Some(Yes))
  val fy2YYyY = fy2YYy.copy(subsidiaryThresholds = Thresholds(Some(Yes)))

  val fy3 = s1.copy(financialYear = Some(FinancialYear.ThirdOrLater))
  val fy3Y = fy3.copy(companyThresholds = Thresholds(Some(Yes)))
  val fy3YY = fy3Y.copy(companyThresholds = Thresholds(Some(Yes), Some(Yes)))
  val fy3N = fy3.copy(companyThresholds = Thresholds(Some(No)))
  val fy3NY = fy3N.copy(companyThresholds = Thresholds(Some(No), Some(Yes)))
  val fy3NN = fy3N.copy(companyThresholds = Thresholds(Some(No), Some(No)))
  val fy3NYN = fy3NY.copy(companyThresholds = Thresholds(Some(No), Some(Yes), Some(No)))
  val fy3NYY = fy3NY.copy(companyThresholds = Thresholds(Some(No), Some(Yes), Some(Yes)))

  val fy3YYn = fy3YY.copy(subsidiaries = Some(No))
  val fy3YYy = fy3YY.copy(subsidiaries = Some(Yes))
  val fy3YYyY = fy3YYy.copy(subsidiaryThresholds = Thresholds(Some(Yes)))

  val expectedDecisions: Seq[(DecisionState, Decision)] = Seq(
    (empty, AskQuestion(Questions.isCompanyOrLLPQuestion)),
    (s0, Exempt(None)),

    (s1, AskQuestion(Questions.financialYearQuestion)),

    (fy1, Exempt(Some("reason.firstyear"))),

    (fy2, AskQuestion(Questions.companyTurnoverQuestionY2)),
    (fy2Y, AskQuestion(Questions.companyBalanceSheetQuestionY2)),
    (fy2N, AskQuestion(Questions.companyBalanceSheetQuestionY2)),
    (fy2NY, AskQuestion(Questions.companyEmployeesQuestionY2)),
    (fy2NN, Exempt(Some("reason.company.notlargeenough"))),
    (fy2NYN, Exempt(Some("reason.company.notlargeenough"))),

    (fy2NYY, AskQuestion(Questions.hasSubsidiariesQuestion)),
    (fy2YY, AskQuestion(Questions.hasSubsidiariesQuestion)),

    (fy2YYy, AskQuestion(Questions.subsidiaryTurnoverQuestionY2)),
    (fy2YYn, Required),
    (fy2YYyY, AskQuestion(Questions.subsidiaryBalanceSheetQuestionY2)),

    (fy3, AskQuestion(Questions.companyTurnoverQuestionY3)),
    (fy3Y, AskQuestion(Questions.companyBalanceSheetQuestionY3)),
    (fy3N, AskQuestion(Questions.companyBalanceSheetQuestionY3)),
    (fy3NY, AskQuestion(Questions.companyEmployeesQuestionY3)),
    (fy3NN, Exempt(Some("reason.company.notlargeenough"))),
    (fy3NYN, Exempt(Some("reason.company.notlargeenough"))),

    (fy3NYY, AskQuestion(Questions.hasSubsidiariesQuestion)),
    (fy3YY, AskQuestion(Questions.hasSubsidiariesQuestion)),

    (fy3YYy, AskQuestion(Questions.subsidiaryTurnoverQuestionY3)),
    (fy3YYn, Required),
    (fy3YYyY, AskQuestion(Questions.subsidiaryBalanceSheetQuestionY3))
  )
}
