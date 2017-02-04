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
  val isCompanyOrLLCQuestion = YesNoQuestion("Is your business a company or Limited Liability Partnership incorporated in the UK?")
  val financialYearQuestion = MultipleChoiceQuestion("")
  val hasSubsidiariesQuestion = YesNoQuestion("")

  val companyTurnoverQuestion = YesNoQuestion("")
  val companyBalanceSheetQuestion = YesNoQuestion("")
  val companyEmployeesQuestion = YesNoQuestion("")
  val companyQuestionGroup = QuestionGroup(companyTurnoverQuestion, companyBalanceSheetQuestion, companyEmployeesQuestion)
  val subsidiaryTurnoverQuestion = YesNoQuestion("")
  val subsidiaryBalanceSheetQuestion = YesNoQuestion("")
  val subsidiaryEmployeesQuestion = YesNoQuestion("")
  val subsidiariesQuestionGroup = QuestionGroup(subsidiaryTurnoverQuestion, subsidiaryBalanceSheetQuestion, subsidiaryEmployeesQuestion)
}
