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

package controllers

import play.api.data.Forms._
import play.api.data.{Forms, Mapping}
import questionnaire._
import utils.YesNo

object QuestionnaireValidations {
  val yesNo: Mapping[YesNo] = Forms.of[YesNo]

  val financialYear: Mapping[FinancialYear] = Forms.of[FinancialYear]

  val yesNoAnswer: Mapping[YesNoAnswer] = mapping(
    "questionId" -> number,
    "answer" -> yesNo
  )(YesNoAnswer.apply)(YesNoAnswer.unapply)

  val yearAnswer: Mapping[FinancialYearAnswer] = mapping(
    "questionId" -> number,
    "answer" -> financialYear
  )(FinancialYearAnswer.apply)(FinancialYearAnswer.unapply)
}
