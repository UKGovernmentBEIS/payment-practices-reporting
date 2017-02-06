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

  private val companyThresholdHint = Some("If your business is part of a group, your answers must be for your business on its own. Every business within the group will need to do this individually")

  val companyTurnoverQuestionY2 = YesNoQuestion(
    "Did your business have a turnover of more than £36 million on its last balance sheet date?",
    companyThresholdHint
  )

  val companyBalanceSheetQuestionY2 = YesNoQuestion(
    "Did your business have a balance sheet total greater than £18 million at its last financial year end?",
    companyThresholdHint
  )
  val companyEmployeesQuestionY2 = YesNoQuestion(
    "Did your business have an average of at least 250 employees during its last financial year?",
    companyThresholdHint
  )
  val companyTurnoverQuestionY3 = YesNoQuestion(
    "Did your business have a turnover of more than £36 million on its last 2 balance sheet dates?",
    companyThresholdHint
  )

  val companyBalanceSheetQuestionY3 = YesNoQuestion(
    "Did your business have a balance sheet total greater than £18 million at its last 2 financial year ends?",
    companyThresholdHint
  )
  val companyEmployeesQuestionY3 = YesNoQuestion(
    "Did your business have an average of at least 250 employees during its last 2 financial years?",
    companyThresholdHint
  )

  val companyQuestionGroupY2 = ThresholdQuestions(companyTurnoverQuestionY2, companyBalanceSheetQuestionY2, companyEmployeesQuestionY2)
  val companyQuestionGroupY3 = ThresholdQuestions(companyTurnoverQuestionY3, companyBalanceSheetQuestionY3, companyEmployeesQuestionY3)

  private val subsidiariesThresholdHint = Some("'Net' here means after any set-offs and other adjustments to exclude group transactions. 'Gross' means without those set-offs and adjustments.")

  val subsidiaryTurnoverQuestionY2 = YesNoQuestion(
    "Did you and your subsidiaries have an total turnover of at least £36 million net or £43.2 million gross on the last balance sheet date?",
    subsidiariesThresholdHint
  )
  val subsidiaryBalanceSheetQuestionY2 = YesNoQuestion(
    "Did you and your subsidiaries have a combined balance sheet total of £18 million net or £21.6 million gross on the last balance sheet date?",
    subsidiariesThresholdHint
  )
  val subsidiaryEmployeesQuestionY2 = YesNoQuestion(
    "Did the you and your subsidiaries have a combined workforce of at least 250 on the last balance sheet date?",
    None
  )



  val subsidiaryTurnoverQuestionY3 = YesNoQuestion(
    "Did you and your subsidiaries have an total turnover of at least £36 million net or £43.2 million gross on both of the last 2 balance sheet dates?",
    subsidiariesThresholdHint
  )
  val subsidiaryBalanceSheetQuestionY3 = YesNoQuestion(
    "Did you and your subsidiaries have a combined balance sheet total of £18 million net or £21.6 million gross on both of the last 2 balance sheet dates?",
    subsidiariesThresholdHint
  )
  val subsidiaryEmployeesQuestionY3 = YesNoQuestion(
    "Did the you and your subsidiaries have a combined workforce of at least 250 on both of the last 2 balance sheet dates?",
    None
  )

  val subsidiariesQuestionGroupY2 = ThresholdQuestions(subsidiaryTurnoverQuestionY2, subsidiaryBalanceSheetQuestionY2, subsidiaryEmployeesQuestionY2)
  val subsidiariesQuestionGroupY3 = ThresholdQuestions(subsidiaryTurnoverQuestionY3, subsidiaryBalanceSheetQuestionY3, subsidiaryEmployeesQuestionY3)
}
