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

object Questions {
  val isCompanyOrLLPQuestion = YesNoQuestion("isCompanyOrLLP", "question.iscompanyorllp", None)
  val financialYearQuestion  = FinancialYearQuestion("financialYear", "question.financialyear", None)

  private val hasSubsidiariesDetail = views.html.questionnaire._hasSubsidiariesReveal()
  val hasSubsidiariesQuestion = YesNoQuestion("subsidiaries", "question.hassubsidiaries", None, Some(hasSubsidiariesDetail))

  private val turnoverHint  = Some("hint.turnover")
  private val balanceHint   = Some("hint.balance")
  private val employeesHint = Some("hint.employees")

  private val balanceSheetTotalDetail = views.html.questionnaire._balanceSheetTotalReveal()

  val companyTurnoverQuestionY2     = YesNoQuestion("companyThresholds.turnover", "question.company.turnover.y2", turnoverHint)
  val companyBalanceSheetQuestionY2 = YesNoQuestion("companyThresholds.balanceSheet", "question.company.balance.y2", balanceHint, Some(balanceSheetTotalDetail))
  val companyEmployeesQuestionY2    = YesNoQuestion("companyThresholds.employees", "question.company.employees.y2", employeesHint)

  val companyTurnoverQuestionY3     = YesNoQuestion("companyThresholds.turnover", "question.company.turnover.y3", turnoverHint)
  val companyBalanceSheetQuestionY3 = YesNoQuestion("companyThresholds.balanceSheet", "question.company.balance.y3", balanceHint, Some(balanceSheetTotalDetail))
  val companyEmployeesQuestionY3    = YesNoQuestion("companyThresholds.employees", "question.company.employees.y3", employeesHint)

  val companyQuestionGroupY2 = ThresholdQuestions(companyTurnoverQuestionY2, companyBalanceSheetQuestionY2, companyEmployeesQuestionY2)
  val companyQuestionGroupY3 = ThresholdQuestions(companyTurnoverQuestionY3, companyBalanceSheetQuestionY3, companyEmployeesQuestionY3)

  private val subsidiariesThresholdHint = Some("hint.subsidiaries")
  private val aggregateDetail           = views.html.questionnaire._aggregationDetail()

  val subsidiaryTurnoverQuestionY2     = YesNoQuestion("subsidiaryThresholds.turnover", "question.subsidiaries.turnover.y2", subsidiariesThresholdHint, Some(aggregateDetail))
  val subsidiaryBalanceSheetQuestionY2 = YesNoQuestion("subsidiaryThresholds.balanceSheet", "question.subsidiaries.balance.y2", subsidiariesThresholdHint, Some(aggregateDetail))
  val subsidiaryEmployeesQuestionY2    = YesNoQuestion("subsidiaryThresholds.employees", "question.subsidiaries.employees.y2", employeesHint)

  val subsidiaryTurnoverQuestionY3     = YesNoQuestion("subsidiaryThresholds.turnover", "question.subsidiaries.turnover.y3", subsidiariesThresholdHint, Some(aggregateDetail))
  val subsidiaryBalanceSheetQuestionY3 = YesNoQuestion("subsidiaryThresholds.balanceSheet", "question.subsidiaries.balance.y3", subsidiariesThresholdHint, Some(aggregateDetail))
  val subsidiaryEmployeesQuestionY3    = YesNoQuestion("subsidiaryThresholds.employees", "question.subsidiaries.employees.y3", employeesHint)

  val subsidiariesQuestionGroupY2 = ThresholdQuestions(subsidiaryTurnoverQuestionY2, subsidiaryBalanceSheetQuestionY2, subsidiaryEmployeesQuestionY2)
  val subsidiariesQuestionGroupY3 = ThresholdQuestions(subsidiaryTurnoverQuestionY3, subsidiaryBalanceSheetQuestionY3, subsidiaryEmployeesQuestionY3)
}
