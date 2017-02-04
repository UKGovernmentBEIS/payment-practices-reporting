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

import play.api.libs.json.Json
import questionnaire.YesNo._

case class DecisionState(
                          isCompanyOrLLP: Option[YesNo],
                          financialYear: Option[FinancialYear],
                          companyAnswers: AnswerGroup,
                          subsidiaries: Option[YesNo],
                          subsidiaryAnswers: AnswerGroup
                        )

object DecisionState {
  val empty: DecisionState = DecisionState(None, None, AnswerGroup.empty, None, AnswerGroup.empty)

  implicit val format = Json.format[DecisionState]
}

object Decider {

  import FinancialYear._
  import Questions._

  def calculateDecision(state: DecisionState): Decision = state.isCompanyOrLLP match {
    case None => AskQuestion(isCompanyOrLLCQuestion)
    case Some(No) => Exempt(None)
    case Some(Yes) => checkFinancialYear(state)
  }

  def checkFinancialYear(state: DecisionState): Decision = state.financialYear match {
    case None => AskQuestion(financialYearQuestion)
    case Some(First) => Exempt(Some("reason.firstyear"))
    case _ => checkCompanyAnswers(state)
  }

  def checkCompanyAnswers(state: DecisionState): Decision = state.companyAnswers.nextQuestion(companyQuestionGroup) match {
    case Some(question) => AskQuestion(question)
    case None if state.companyAnswers.score >= 2 => checkIfSubsidiaries(state)
    case None => Exempt(Some("reason.company.notlargeenough"))
  }

  def checkIfSubsidiaries(state: DecisionState): Decision = state.subsidiaries match {
    case None => AskQuestion(hasSubsidiariesQuestion)
    case Some(No) => Required
    case Some(Yes) => checkSubsidiaryAnswers(state)
  }

  def checkSubsidiaryAnswers(state: DecisionState): Decision = state.subsidiaryAnswers.nextQuestion(companyQuestionGroup) match {
    case Some(question) => AskQuestion(question)
    case None if state.companyAnswers.score >= 2 => Required
    case None => Exempt(Some("reason.group.notlargeenough"))
  }
}