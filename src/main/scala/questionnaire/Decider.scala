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


case class DecisionState(
                          isCompanyOrLLP: Option[YesNo],
                          financialYear: Option[FinancialYear],
                          companyThresholds: Thresholds,
                          subsidiaries: Option[YesNo],
                          subsidiaryThresholds: Thresholds
                        )

object DecisionState {
  val empty: DecisionState = DecisionState(None, None, Thresholds.empty, None, Thresholds.empty)
}

sealed trait Decision

case class AskQuestion(key: String, question: Question) extends Decision

case class Exempt(reason: Option[String]) extends Decision

case object Required extends Decision

class Decider @Inject()(questions: Questions){

  import FinancialYear._
  import YesNo._
  import questions._

  def calculateDecision(state: DecisionState): Decision = state.isCompanyOrLLP match {
    case None => AskQuestion("isCompanyOrLLP", isCompanyOrLLPQuestion)
    case Some(No) => Exempt(None)
    case Some(Yes) => checkFinancialYear(state)
  }

  def checkFinancialYear(state: DecisionState): Decision = state.financialYear match {
    case None => AskQuestion("financialYear", financialYearQuestion)
    case Some(First) => Exempt(Some("reason.firstyear"))
    case _ => checkCompanyThresholds(state)
  }

  def companyQuestionGroupForFY(financialYear: FinancialYear) = financialYear match {
    case ThirdOrLater => companyQuestionGroupY3
    case _ => companyQuestionGroupY2
  }

  def checkCompanyThresholds(state: DecisionState): Decision =
    state.companyThresholds.nextQuestion(companyQuestionGroupForFY(state.financialYear.getOrElse(Second))) match {
      case _ if state.companyThresholds.score >= 2 => checkIfSubsidiaries(state)
      case Some(AskQuestion(key, q)) => AskQuestion(s"companyThresholds.$key", q)
      case None => Exempt(Some("reason.company.notlargeenough"))
    }

  def checkIfSubsidiaries(state: DecisionState): Decision = state.subsidiaries match {
    case None => AskQuestion("subsidiaries", hasSubsidiariesQuestion)
    case Some(No) => Required
    case Some(Yes) => checkSubsidiaryThresholds(state)
  }

  def subsidiariesQuestionGroupForFY(financialYear: FinancialYear) = financialYear match {
    case ThirdOrLater => subsidiariesQuestionGroupY3
    case _ => subsidiariesQuestionGroupY2
  }

  def checkSubsidiaryThresholds(state: DecisionState): Decision =
    state.subsidiaryThresholds.nextQuestion(subsidiariesQuestionGroupForFY(state.financialYear.getOrElse(Second))) match {
      case _ if state.subsidiaryThresholds.score >= 2 => Required
      case Some(AskQuestion(key, q)) => AskQuestion(s"subsidiaryThresholds.$key", q)
      case None => Exempt(Some("reason.group.notlargeenough"))
    }
}