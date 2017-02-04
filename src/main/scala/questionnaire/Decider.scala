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
import questionnaire.Answer._

case class DecisionState(
                          isCompanyOrLLP: Answer,
                          financialYear: FinancialYear,
                          companyAnswers: AnswerGroup,
                          subsidiaries: Answer,
                          subsidiaryAnswers: AnswerGroup
                        )

object DecisionState {
  val empty: DecisionState = DecisionState(Unanswered, FinancialYear.Unknown, AnswerGroup.empty, Unanswered, AnswerGroup.empty)

  implicit val format = Json.format[DecisionState]
}

object Decider {

  import FinancialYear._
  import Questions._

  def calculateDecision(state: DecisionState): Decision = state.isCompanyOrLLP match {
    case Unanswered => AskQuestion(isCompanyOrLLCQuestion)
    case No => Exempt(None)
    case Yes => checkFinancialYear(state)
  }

  def checkFinancialYear(state: DecisionState): Decision = state.financialYear match {
    case Unknown => AskQuestion(financialYearQuestion)
    case First => Exempt(Some("reason.firstyear"))
    case Second | ThirdOrLater => checkCompanyAnswers(state)
  }

  def checkCompanyAnswers(state: DecisionState): Decision = state.companyAnswers.nextQuestion(companyQuestionGroup) match {
    case Some(question) => AskQuestion(question)
    case None if state.companyAnswers.score >= 2 => checkIfSubsidiaries(state)
    case None => Exempt(Some("reason.company.notlargeenough"))
  }

  def checkIfSubsidiaries(state: DecisionState): Decision = state.subsidiaries match {
    case Unanswered => AskQuestion(hasSubsidiariesQuestion)
    case No => Required
    case Yes => checkSubsidiaryAnswers(state)
  }

  def checkSubsidiaryAnswers(state: DecisionState): Decision = state.subsidiaryAnswers.nextQuestion(companyQuestionGroup) match {
    case Some(question) => AskQuestion(question)
    case None if state.companyAnswers.score >= 2 => Required
    case None => Exempt(Some("reason.group.notlargeenough"))
  }
}