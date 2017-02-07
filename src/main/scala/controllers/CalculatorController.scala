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
import org.joda.time.format.DateTimeFormat
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}

class CalculatorController @Inject()(implicit messages: MessagesApi) extends Controller with PageHelper {

  import forms.Validations.financialPeriod

  val emptyForm = Form[FinancialYear](financialPeriod)

  val df = DateTimeFormat.forPattern("d MMMM YYYY")

  def calculatorPage(form: Form[FinancialYear]) = page(home, views.html.calculator.calculator(form))

  def show = Action(Ok(calculatorPage(emptyForm)))

  def submit = Action { implicit request =>
    emptyForm.bindFromRequest().fold(
      formWithErrs => Ok(calculatorPage(discardErrorsIfEmpty(formWithErrs))),
      fy => Redirect(controllers.routes.CalculatorController.showAnswer()).flashing(financialPeriod.unbind(fy).toSeq: _*)
    )
  }

  def showAnswer = Action { implicit request =>
    financialPeriod.bind(request.flash.data).fold(
      _ => Redirect(controllers.routes.CalculatorController.show()),
      fy => Ok(page(home, views.html.calculator.answer(isGroup = false, Calculator(fy), df)))
    )

  }
}
