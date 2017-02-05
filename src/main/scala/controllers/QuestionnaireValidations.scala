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

package controllers

import play.api.data.Forms.{mapping, optional}
import play.api.data.{Forms, Mapping}
import questionnaire.{DecisionState, FinancialYear, Thresholds, YesNo}

object QuestionnaireValidations {
  val yesNo: Mapping[YesNo] = Forms.of[YesNo]

  val financialYear: Mapping[FinancialYear] = Forms.of[FinancialYear]

  val thresholds: Mapping[Thresholds] = mapping(
    "turnover" -> optional(yesNo),
    "balanceSheet" -> optional(yesNo),
    "employees" -> optional(yesNo)
  )(Thresholds.apply)(Thresholds.unapply)

  val decisionStateMapping: Mapping[DecisionState] = mapping(
    "isCompanyOrLLP" -> optional(yesNo),
    "financialYear" -> optional(financialYear),
    "companyThresholds" -> thresholds,
    "subsidiaries" -> optional(yesNo),
    "subsidiaryThresholds" -> thresholds
  )(DecisionState.apply)(DecisionState.unapply)

  val emptyState = decisionStateMapping.unbind(DecisionState.empty)
}
