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

import questionnaire.{FinancialYear, Thresholds}
import utils.YesNo

/**
  * This models the set of answers that have been provided to the questionnaire questions. It is used by
  * the `Decider` to decide on the next action to take (ask another question or show a result) and by
  * the `Summarizer` to decide which reason messages to show the user when they need to report.
  */
case class DecisionState(
                          isCompanyOrLLP: Option[YesNo],
                          financialYear: Option[FinancialYear],
                          companyThresholds: Thresholds,
                          subsidiaries: Option[YesNo],
                          subsidiaryThresholds: Thresholds
                        )

object DecisionState {
  val empty: DecisionState = models.DecisionState(None, None, Thresholds.empty, None, Thresholds.empty)
}