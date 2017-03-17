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

import models.DecisionState
import play.api.i18n.MessagesApi
import utils.YesNo

case class ThresholdSummary(turnover: Option[String], balance: Option[String], employees: Option[String])

case class StateSummary(companyOrLLP: Option[String], companyThresholdSummary: ThresholdSummary, groupThresholdSummary: ThresholdSummary)

class Summarizer @Inject()(messages: MessagesApi) {

  def ifYes(q: Option[YesNo], messageKey: String): Option[String] = q.flatMap {
    case YesNo.Yes => Some(messages(messageKey))
    case _ => None
  }

  def keyForFinancialYear(decisionState: DecisionState, keyBase: String): String = decisionState.financialYear match {
    case Some(FinancialYear.ThirdOrLater) => s"$keyBase.y3"
    case _ => s"$keyBase.y2"
  }

  def summarize(decisionState: DecisionState): StateSummary = {
    StateSummary(
      ifYes(decisionState.isCompanyOrLLP, "summary.iscompanyorllp"),
      ThresholdSummary(
        ifYes(decisionState.companyThresholds.turnover, keyForFinancialYear(decisionState, "summary.company.turnover")),
        ifYes(decisionState.companyThresholds.balanceSheet, keyForFinancialYear(decisionState, "summary.company.balance")),
        ifYes(decisionState.companyThresholds.employees, keyForFinancialYear(decisionState, "summary.company.employees"))
      ),
      ThresholdSummary(
        ifYes(decisionState.subsidiaryThresholds.turnover, keyForFinancialYear(decisionState, "summary.subsidiaries.turnover")),
        ifYes(decisionState.subsidiaryThresholds.balanceSheet, keyForFinancialYear(decisionState, "summary.subsidiaries.balance")),
        ifYes(decisionState.subsidiaryThresholds.employees, keyForFinancialYear(decisionState, "summary.subsidiaries.employees"))
      )
    )

  }
}
