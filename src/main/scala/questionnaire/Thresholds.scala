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

import org.scalactic.TripleEquals._
import play.api.Logger

case class Thresholds(turnover: Option[YesNo], balanceSheet: Option[YesNo], employees: Option[YesNo]) {
  def score: Int = {
    Logger.debug(s"checking score of $this")
    val s = Seq(turnover, balanceSheet, employees).flatten.count(_ === YesNo.Yes)
    Logger.debug(s"score is $s")
    s
  }

  def nextQuestion(questionGroup: ThresholdQuestions): Option[AskQuestion] = (turnover, balanceSheet, employees) match {
    case (None, _, _) => Some(AskQuestion("turnover", questionGroup.turnoverQuestion))
    case (Some(_), None, _) => Some(AskQuestion("balanceSheet", questionGroup.balanceSheetQuestion))
    case (Some(_), Some(_), None) => Some(AskQuestion("employees", questionGroup.employeesQuestion))
    case _ => None
  }
}

object Thresholds {
  val empty: Thresholds = Thresholds(None, None, None)
}