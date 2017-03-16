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

import utils.YesNo

case class DecisionState(
                          isCompanyOrLLP: Option[YesNo],
                          financialYear: Option[FinancialYear],
                          companyThresholds: Thresholds,
                          subsidiaries: Option[YesNo],
                          subsidiaryThresholds: Thresholds
                        )

object DecisionState {
  val empty: DecisionState = DecisionState(None, None, Thresholds.empty, None, Thresholds.empty)

  import YesNo.Yes

  val secondYear: DecisionState = DecisionState(Some(Yes), Some(FinancialYear.Second), Thresholds(Some(Yes), Some(Yes), Some(Yes)), Some(Yes), Thresholds(Some(Yes), Some(Yes), Some(Yes)))
  val thirdYear: DecisionState = DecisionState(Some(Yes), Some(FinancialYear.ThirdOrLater), Thresholds(Some(Yes), Some(Yes), Some(Yes)), Some(Yes), Thresholds(Some(Yes), Some(Yes), Some(Yes)))
}

sealed trait Decision

case class AskQuestion(question: Question) extends Decision

case class Exempt(reason: Option[String]) extends Decision

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
    case Some(No) => Exempt(None)
    case Some(Yes) => checkFinancialYear(state)
  }

  def checkFinancialYear(state: DecisionState): Decision = state.financialYear match {
    case None => AskQuestion(financialYearQuestion)
    case Some(First) => Exempt(Some("reason.firstyear"))
    case Some(fy) => checkCompanyThresholds(state, fy)
  }

  def companyQuestionGroupForFY(financialYear: FinancialYear) = financialYear match {
    case ThirdOrLater => companyQuestionGroupY3
    case _ => companyQuestionGroupY2
  }

  val companyNotLargeEnough = "reason.company.notlargeenough"

  def checkCompanyThresholds(state: DecisionState, financialYear: FinancialYear): Decision = {
    if (state.companyThresholds.yesCount >= 2) checkIfSubsidiaries(state, financialYear)
    else if (state.companyThresholds.noCount >= 2) Exempt(Some(companyNotLargeEnough))
    else state.companyThresholds.nextQuestion(companyQuestionGroupForFY(financialYear)) match {
      case Some(aq) => aq
      case None => ??? // impossible state!
    }
  }

  def checkIfSubsidiaries(state: DecisionState, financialYear: FinancialYear): Decision = state.subsidiaries match {
    case None => AskQuestion(hasSubsidiariesQuestion)
    case Some(No) => Required
    case Some(Yes) => checkSubsidiaryThresholds(state, financialYear)
  }

  def subsidiariesQuestionGroupForFY(financialYear: FinancialYear) = financialYear match {
    case ThirdOrLater => subsidiariesQuestionGroupY3
    case _ => subsidiariesQuestionGroupY2
  }

  val groupNotLargeEnough = "reason.group.notlargeenough"

  def checkSubsidiaryThresholds(state: DecisionState, financialYear: FinancialYear): Decision = {
    if (state.subsidiaryThresholds.yesCount >= 2) Required
    else if (state.subsidiaryThresholds.noCount >= 2) Exempt(Some(groupNotLargeEnough))
    else state.subsidiaryThresholds.nextQuestion(subsidiariesQuestionGroupForFY(financialYear)) match {
      case Some(aq) => aq
      case None => ??? // impossible state!
    }
  }
}