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
import forms.DateRange
import org.joda.time.format.DateTimeFormat
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}

class CalculatorController @Inject()(implicit messages: MessagesApi) extends Controller with PageHelper {

  import forms.Validations._

  val emptyForm = Form[DateRange](dateRange)

  val df = DateTimeFormat.forPattern("d MMMM YYYY")

  def calculatorPage(form: Form[DateRange]) = page(home, views.html.calculator.calculator(form))

  def show = Action { implicit request =>
    Ok(calculatorPage(emptyForm)).removing(financialYear)
  }

  def submit = Action { implicit request =>
    emptyForm.bindFromRequest().fold(
      formWithErrs => BadRequest(calculatorPage(discardErrorsIfEmpty(formWithErrs))),
      dr => Redirect(controllers.routes.CalculatorController.showAnswer())
        .addingToSession(financialYear.unbind(FinancialYear(dr)).toSeq: _*)
    )
  }

  def showAnswer = Action { implicit request =>
    financialYear.bind(request.session.data).fold(
      _ => Redirect(controllers.routes.CalculatorController.show()),
      fy => Ok(page(home, views.html.calculator.answer(isGroup = false, Calculator(fy), df)))
    )
  }
}
