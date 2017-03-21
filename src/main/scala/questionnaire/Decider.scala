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

import models.{DecisionState, Question, ThresholdQuestions}
import utils.YesNo

sealed trait Decision

case class AskQuestion(question: Question) extends Decision

case class Exempt(reason: Option[String]) extends Decision

case object NotACompany extends Decision

case object Required extends Decision

/**
  * This class implements the decision tree to decide whether a business needs to report or not. This is based
  * on the legislation, which can be found here http://www.legislation.gov.uk/ukdsi/2017/9780111153598/regulation/5
  */
object Decider {

  import FinancialYear._
  import Questions._
  import YesNo._

  def calculateDecision(state: DecisionState): Decision = state.isCompanyOrLLP match {
    case None => AskQuestion(isCompanyOrLLPQuestion)
    case Some(No) => NotACompany
    case Some(Yes) => checkFinancialYear(state)
  }

  def checkFinancialYear(state: DecisionState): Decision = state.financialYear match {
    case None => AskQuestion(financialYearQuestion)
    case Some(First) => Exempt(Some("reason.firstyear"))
    case Some(fy) => checkCompanyThresholds(state, fy)
  }

  def companyQuestions(financialYear: FinancialYear) = financialYear match {
    case ThirdOrLater => companyQuestionGroupY3
    case _ => companyQuestionGroupY2
  }

  val companyNotLargeEnough = "reason.company.notlargeenough"

  def checkCompanyThresholds(state: DecisionState, financialYear: FinancialYear): Decision = decideThresholds(
    state.companyThresholds,
    companyQuestions(financialYear),
    checkIfSubsidiaries(state, financialYear),
    Exempt(Some(companyNotLargeEnough)))


  def checkIfSubsidiaries(state: DecisionState, financialYear: FinancialYear): Decision = state.subsidiaries match {
    case None => AskQuestion(hasSubsidiariesQuestion)
    case Some(No) => Required
    case Some(Yes) => checkSubsidiaryThresholds(state, financialYear)
  }

  def subsidiariesQuestions(financialYear: FinancialYear) = financialYear match {
    case ThirdOrLater => subsidiariesQuestionGroupY3
    case _ => subsidiariesQuestionGroupY2
  }

  val groupNotLargeEnough = "reason.group.notlargeenough"

  def checkSubsidiaryThresholds(state: DecisionState, financialYear: FinancialYear): Decision = decideThresholds(
    state.subsidiaryThresholds,
    subsidiariesQuestions(financialYear),
    Required,
    Exempt(Some(groupNotLargeEnough)))

  private def decideThresholds(answers: Thresholds, questions: ThresholdQuestions, yesesHaveIt: => Decision, noesHaveIt: => Decision) = {
    answers match {
      // See if there are two Yeses
      case Thresholds(Some(Yes), Some(Yes), _) |
           Thresholds(Some(Yes), _, Some(Yes)) |
           Thresholds(_, Some(Yes), Some(Yes)) => yesesHaveIt

      // See if there are two Noes
      case Thresholds(Some(No), Some(No), _) |
           Thresholds(Some(No), _, Some(No)) |
           Thresholds(_, Some(No), Some(No)) => noesHaveIt

      case Thresholds(None, _, _) => AskQuestion(questions.turnoverQuestion)
      case Thresholds(Some(_), None, _) => AskQuestion(questions.balanceSheetQuestion)
      case Thresholds(Some(_), Some(_), None) => AskQuestion(questions.employeesQuestion)
    }
  }
}