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
  val isCompanyOrLLPQuestion = YesNoQuestion("Is your business a company or Limited Liability Partnership incorporated in the UK?", None)
  val financialYearQuestion = MultipleChoiceQuestion("Which financial year is your business currently in?", None,
    Seq(Choice("First year", FinancialYear.First.entryName),
      Choice("Second year", FinancialYear.Second.entryName),
      Choice("Third year or later", FinancialYear.ThirdOrLater.entryName)
    ))

  val hasSubsidiariesQuestion = YesNoQuestion("Does your company have subsidiaries?", None)

  val companyTurnoverQuestion = YesNoQuestion(
    "Did your business have a turnover of more than £36 million on its last balance sheet date?",
    Some("If your business is part of a group, your answers must be for your business on its own. Every business within the group will need to do this individually")
  )

  val companyBalanceSheetQuestion = YesNoQuestion(
    "Did your business have a balance sheet total greater than £18 million at its last financial year end?",
    Some("If your business is part of a group, your answers must be for your business on its own. Every business within the group will need to do this individually")
  )
  val companyEmployeesQuestion = YesNoQuestion(
    "Did your business have an average of at least 250 employees during its last financial year?",
    Some("If your business is part of a group, your answers must be for your business on its own. Every business within the group will need to do this individually")
  )

  val companyQuestionGroup = ThresholdQuestions(companyTurnoverQuestion, companyBalanceSheetQuestion, companyEmployeesQuestion)
  val subsidiaryTurnoverQuestion = YesNoQuestion("", None)
  val subsidiaryBalanceSheetQuestion = YesNoQuestion("", None)
  val subsidiaryEmployeesQuestion = YesNoQuestion("", None)
  val subsidiariesQuestionGroup = ThresholdQuestions(subsidiaryTurnoverQuestion, subsidiaryBalanceSheetQuestion, subsidiaryEmployeesQuestion)
}
