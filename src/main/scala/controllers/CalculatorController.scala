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

import forms.DateRange
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}

class CalculatorController @Inject()(implicit messages: MessagesApi) extends Controller with PageHelper {

  val emptyForm = Form[DateRange](forms.Validations.dateRange)

  def show = Action {
    Ok(page(views.html.calculator(emptyForm)))
  }


  /**
    * If all the fields are empty then don't report any errors
    */
  private def discardErrorsIfEmpty(form: Form[DateRange]) =
    if (form.data.exists(_._2 != "")) form else form.discardingErrors

  def submit = Action { implicit request =>
    emptyForm.bindFromRequest().fold(
      formWithErrs => Ok(page(views.html.calculator(discardErrorsIfEmpty(formWithErrs)))),
      _ => Redirect(controllers.routes.HomeController.index())
    )
  }
}
