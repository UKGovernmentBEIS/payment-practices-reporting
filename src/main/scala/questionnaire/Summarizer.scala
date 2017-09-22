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

import javax.inject.Inject

import monocle.macros.Lenses
import play.api.i18n.MessagesApi
import utils.YesNo
import utils.YesNo.Yes

@Lenses
case class ThresholdSummary(turnover: Option[String], balance: Option[String], employees: Option[String])

object ThresholdSummary {
  val empty: ThresholdSummary = ThresholdSummary(None, None, None)
}

@Lenses
case class StateSummary(companyOrLLP: Option[String], companyThresholdSummary: ThresholdSummary, groupThresholdSummary: ThresholdSummary)

object StateSummary {
  val empty: StateSummary = StateSummary.this (None, ThresholdSummary.empty, ThresholdSummary.empty)
}

class Summarizer @Inject()(messages: MessagesApi) {
  def keyForFinancialYear(decisionState: DecisionState, keyBase: String): String = decisionState.financialYear match {
    case Some(FinancialYear.ThirdOrLater) => s"$keyBase.y3"
    case _                                => s"$keyBase.y2"
  }

  import Questions._

  private val companyTurnover  = StateSummary.companyThresholdSummary composeLens ThresholdSummary.turnover
  private val companyBalance   = StateSummary.companyThresholdSummary composeLens ThresholdSummary.balance
  private val companyEmployees = StateSummary.companyThresholdSummary composeLens ThresholdSummary.employees

  private val groupTurnover  = StateSummary.groupThresholdSummary composeLens ThresholdSummary.turnover
  private val groupBalance   = StateSummary.groupThresholdSummary composeLens ThresholdSummary.balance
  private val groupEmployees = StateSummary.groupThresholdSummary composeLens ThresholdSummary.employees

  def summarize(answers: Seq[Answer]): StateSummary = {
    answers.foldLeft(StateSummary.empty) { case (summary, answer) =>
      answer match {
        case YesNoAnswer(isCompanyOrLLPQuestion.id, Yes) => summary.copy(companyOrLLP = Some(messages("summary.iscompanyorllp")))

        case YesNoAnswer(companyTurnoverQuestionY2.id, Yes)     => companyTurnover.set(Some(messages("summary.company.turnover.y2")))(summary)
        case YesNoAnswer(companyTurnoverQuestionY3.id, Yes)     => companyTurnover.set(Some(messages("summary.company.turnover.y3")))(summary)

        case YesNoAnswer(companyBalanceSheetQuestionY2.id, Yes) => companyBalance.set(Some(messages("summary.company.balance.y2")))(summary)
        case YesNoAnswer(companyBalanceSheetQuestionY3.id, Yes) => companyBalance.set(Some(messages("summary.company.balance.y3")))(summary)

        case YesNoAnswer(companyEmployeesQuestionY2.id, Yes)    => companyEmployees.set(Some(messages("summary.company.employees.y2")))(summary)
        case YesNoAnswer(companyEmployeesQuestionY3.id, Yes)    => companyEmployees.set(Some(messages("summary.company.employees.y3")))(summary)

        case YesNoAnswer(subsidiaryTurnoverQuestionY2.id, Yes)     => groupTurnover.set(Some(messages("summary.subsidiaries.turnover.y2")))(summary)
        case YesNoAnswer(subsidiaryTurnoverQuestionY3.id, Yes)     => groupTurnover.set(Some(messages("summary.subsidiaries.turnover.y3")))(summary)

        case YesNoAnswer(subsidiaryBalanceSheetQuestionY2.id, Yes) => groupBalance.set(Some(messages("summary.subsidiaries.balance.y2")))(summary)
        case YesNoAnswer(subsidiaryBalanceSheetQuestionY3.id, Yes) => groupBalance.set(Some(messages("summary.subsidiaries.balance.y3")))(summary)

        case YesNoAnswer(subsidiaryEmployeesQuestionY2.id, Yes)    => groupEmployees.set(Some(messages("summary.subsidiaries.employees.y2")))(summary)
        case YesNoAnswer(subsidiaryEmployeesQuestionY3.id, Yes)    => groupEmployees.set(Some(messages("summary.subsidiaries.employees.y3")))(summary)

        case _ => summary
      }
    }
  }
}
