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

import play.api.i18n.MessagesApi

class Questions @Inject()(implicit messages: MessagesApi) {
  val isCompanyOrLLPQuestion = YesNoQuestion(messages("question.iscompanyorllp"), None)
  val financialYearQuestion = MultipleChoiceQuestion(messages("question.financialyear"), None,
    Seq(Choice(messages("choice.first"), FinancialYear.First.entryName),
      Choice(messages("choice.second"), FinancialYear.Second.entryName),
      Choice(messages("choice.third"), FinancialYear.ThirdOrLater.entryName)
    ))

  val hasSubsidiariesDetail = views.html.questionnaire._hasSubsidiariesReveal()
  val hasSubsidiariesQuestion = YesNoQuestion(messages("question.hassubsidiaries"), None, Some(hasSubsidiariesDetail))

  private val turnoverHint = Some(messages("hint.turnover"))
  private val balanceHint = Some(messages("hint.balance"))
  private val employeesHint = Some(messages("hint.employees"))

  val companyTurnoverQuestionY2 = YesNoQuestion(messages("question.company.turnover.y2"), turnoverHint)
  val companyBalanceSheetQuestionY2 = YesNoQuestion(messages("question.company.balance.y2"), balanceHint)
  val companyEmployeesQuestionY2 = YesNoQuestion(messages("question.company.employees.y2"), employeesHint)

  val companyTurnoverQuestionY3 = YesNoQuestion(messages("question.company.turnover.y3"), turnoverHint)
  val companyBalanceSheetQuestionY3 = YesNoQuestion(messages("question.company.balance.y3"), balanceHint)
  val companyEmployeesQuestionY3 = YesNoQuestion(messages("question.company.employees.y3"), employeesHint)

  val companyQuestionGroupY2 = ThresholdQuestions(companyTurnoverQuestionY2, companyBalanceSheetQuestionY2, companyEmployeesQuestionY2)
  val companyQuestionGroupY3 = ThresholdQuestions(companyTurnoverQuestionY3, companyBalanceSheetQuestionY3, companyEmployeesQuestionY3)

  private val subsidiariesThresholdHint = Some(messages("hint.subsidiaries"))

  val subsidiaryTurnoverQuestionY2 = YesNoQuestion(messages("question.subsidiaries.turnover.y2"), subsidiariesThresholdHint)
  val subsidiaryBalanceSheetQuestionY2 = YesNoQuestion(messages("question.subsidiaries.balance.y2"), subsidiariesThresholdHint)
  val subsidiaryEmployeesQuestionY2 = YesNoQuestion(messages("question.subsidiaries.employees.y2"), employeesHint)

  val subsidiaryTurnoverQuestionY3 = YesNoQuestion(messages("question.subsidiaries.turnover.y3"), subsidiariesThresholdHint)
  val subsidiaryBalanceSheetQuestionY3 = YesNoQuestion(messages("question.subsidiaries.balance.y3"), subsidiariesThresholdHint)
  val subsidiaryEmployeesQuestionY3 = YesNoQuestion(messages("question.subsidiaries.employees.y3"), employeesHint)

  val subsidiariesQuestionGroupY2 = ThresholdQuestions(subsidiaryTurnoverQuestionY2, subsidiaryBalanceSheetQuestionY2, subsidiaryEmployeesQuestionY2)
  val subsidiariesQuestionGroupY3 = ThresholdQuestions(subsidiaryTurnoverQuestionY3, subsidiaryBalanceSheetQuestionY3, subsidiaryEmployeesQuestionY3)
}
