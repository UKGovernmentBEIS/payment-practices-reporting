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

import javax.inject.Inject

import calculator.{Calculator, FinancialYear}
import config.GoogleAnalyticsConfig
import forms.DateRange
import forms.Validations.dateRange
import org.joda.time.format.DateTimeFormat
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller, RequestHeader}

class CalculatorController @Inject()(implicit messages: MessagesApi, val googleAnalytics: GoogleAnalyticsConfig) extends Controller with PageHelper {

  import CalculatorController._

  def calculatorPage(form: Form[DateRange])(implicit rh: RequestHeader) = page("Calculate reporting periods and deadlines")(home, views.html.calculator.calculator(form))

  def start = Action { implicit request =>
    Ok(calculatorPage(emptyForm))
  }

  def calculate = Action { implicit request =>
    emptyForm.bindFromRequest().fold(
      formWithErrs => BadRequest(calculatorPage(discardErrorsIfEmpty(formWithErrs))),
      dr => Ok(page("Reporting periods and deadlines")(home, views.html.calculator.answer(isGroup = false, Calculator(FinancialYear(dr)), df)))
    )
  }
}

object CalculatorController {
  val emptyForm = Form[DateRange](dateRange)

  val df = DateTimeFormat.forPattern("d MMMM YYYY")

}
