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

  val s2a = s1.copy(financialYear = Some(FinancialYear.First))

  val s2b = s1.copy(financialYear = Some(FinancialYear.Second))
  val s3b = s2b.copy(companyThresholds = s2b.companyThresholds.copy(turnover = Some(Yes)))
  val s4b = s3b.copy(companyThresholds = s3b.companyThresholds.copy(balanceSheet = Some(Yes)))

  val s2c = s1.copy(financialYear = Some(FinancialYear.ThirdOrLater))
  val s3c = s2c.copy(companyThresholds = s2c.companyThresholds.copy(turnover = Some(Yes)))
  val s4c = s3c.copy(companyThresholds = s3c.companyThresholds.copy(balanceSheet = Some(Yes)))

  val expectedDecisions: Seq[(DecisionState, Decision)] = Seq(
    (empty, AskQuestion(Questions.isCompanyOrLLPQuestion)),
    (s0, Exempt(None)),

    (s1, AskQuestion(Questions.financialYearQuestion)),

    (s2a, Exempt(Some("reason.firstyear"))),

    (s2b, AskQuestion(Questions.companyTurnoverQuestionY2)),
    (s3b, AskQuestion(Questions.companyBalanceSheetQuestionY2)),
    (s4b, AskQuestion(Questions.hasSubsidiariesQuestion)),

    (s2c, AskQuestion(Questions.companyTurnoverQuestionY3)),
    (s3c, AskQuestion(Questions.companyBalanceSheetQuestionY3)),
    (s4c, AskQuestion(Questions.hasSubsidiariesQuestion))
  )
}
