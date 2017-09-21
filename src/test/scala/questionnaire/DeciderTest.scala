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


  "check decision for basic states" in {
    val table = Table("basic states", basicData: _*)
    forAll(table) { case (state, expectedDecision) =>
      Decider.calculateDecision(state) shouldBe expectedDecision
    }
  }

  "check decision for year 2 states" in {
    val table = Table("year 2 states", year2Data: _*)
    forAll(table) { case (state, expectedDecision) =>
      Decider.calculateDecision(state) shouldBe expectedDecision
    }
  }

  "check decision for year 3 states" in {
    val table = Table("year 3 states", year3data: _*)
    forAll(table) { case (state, expectedDecision) =>
      Decider.calculateDecision(state) shouldBe expectedDecision
    }
  }
}

object DeciderTestData {

  import utils.YesNo._

  val empty = DecisionState.empty

  val basicData: Seq[(DecisionState, Either[Question, Decision])] = Seq(
    (empty, Left(Questions.isCompanyOrLLPQuestion)),
    (empty.copy(isCompanyOrLLP = Some(No)), Right(NotACompany("reason.notacompany"))),
    (empty.copy(isCompanyOrLLP = Some(Yes)), Left(Questions.financialYearQuestion)),
    (empty.copy(isCompanyOrLLP = Some(Yes)).copy(financialYear = Some(FinancialYear.First)), Right(Exempt("reason.firstyear")))
  )

  val year2Data = new FYData(FinancialYear.Second, Questions.companyQuestionGroupY2, Questions.subsidiariesQuestionGroupY2).expectedDecisions
  val year3data = new FYData(FinancialYear.ThirdOrLater, Questions.companyQuestionGroupY3, Questions.subsidiariesQuestionGroupY3).expectedDecisions
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
  val YYyN = YYy.copy(subsidiaryThresholds = Thresholds(Some(No)))
  val YYyNY = YYy.copy(subsidiaryThresholds = Thresholds(Some(No), Some(Yes)))
  val YYyYY = YYyY.copy(subsidiaryThresholds = Thresholds(Some(Yes), Some(Yes)))
  val YYyYN = YYyY.copy(subsidiaryThresholds = Thresholds(Some(Yes), Some(No)))
  val YYyYNN = YYyY.copy(subsidiaryThresholds = Thresholds(Some(Yes), Some(No), Some(No)))
  val YYyYNY = YYyYN.copy(subsidiaryThresholds = Thresholds(Some(Yes), Some(No), Some(Yes)))
  val YYyNYY = YYyNY.copy(subsidiaryThresholds = Thresholds(Some(No), Some(Yes), Some(Yes)))
  val YYyNYN = YYyNY.copy(subsidiaryThresholds = Thresholds(Some(No), Some(Yes), Some(No)))

  val expectedDecisions: Seq[(DecisionState, Either[Question, Decision])] =
    Seq(
      (startState, Left(companyQuestions.turnoverQuestion)),
      (Y, Left(companyQuestions.balanceSheetQuestion)),
      (N, Left(companyQuestions.balanceSheetQuestion)),
      (NY, Left(companyQuestions.employeesQuestion)),
      (NN, Right(Exempt("reason.company.notlargeenough"))),
      (NYN, Right(Exempt("reason.company.notlargeenough"))),

      (NYY, Left(Questions.hasSubsidiariesQuestion)),
      (YY, Left(Questions.hasSubsidiariesQuestion)),

      (YYy, Left(subsidiaryQuestions.turnoverQuestion)),
      (YYn, Right(Required)),
      (YYyY, Left(subsidiaryQuestions.balanceSheetQuestion)),
      (YYyYY, Right(Required)),
      (YYyYN, Left(subsidiaryQuestions.employeesQuestion)),
      (YYyYNN, Right(Exempt("reason.group.notlargeenough"))),
      (YYyNYN, Right(Exempt("reason.group.notlargeenough"))),
      (YYyYNY, Right(Required)),
      (YYyNYY, Right(Required))
    )
}
