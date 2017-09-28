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
import config.{PageConfig, ServiceConfig}
import forms.DateRange
import forms.Validations.dateRange
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller, RequestHeader}

class CalculatorController @Inject()(implicit messages: MessagesApi,
                                     val pageConfig: PageConfig,
                                     val serviceConfig: ServiceConfig) extends Controller with PageHelper {

  import CalculatorController._
  private val back = backCrumb(routes.CalculatorController.start().url)

  //noinspection TypeAnnotation
  def calculatorPage(form: Form[DateRange])(implicit rh: RequestHeader) =
    page("Calculate reporting periods and deadlines")(home, views.html.calculator.calculator(form, implicitly[ExternalRouter]))

  def start = Action { implicit request =>
    Ok(calculatorPage(emptyForm))
  }

  def calculate = Action { implicit request =>
    emptyForm.bindFromRequest().fold(
      formWithErrs => BadRequest(calculatorPage(discardErrorsIfEmpty(formWithErrs))),
      dr => Ok(page("Reporting periods and deadlines")(back, views.html.calculator.answer(isGroup = false, Calculator(FinancialYear(dr)), df)))
    )
  }
}

object CalculatorController {
  val emptyForm: Form[DateRange] = Form[DateRange](dateRange)

  val df: DateTimeFormatter = DateTimeFormat.forPattern("d MMMM YYYY")

}
