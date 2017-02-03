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

import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}

class QuestionnaireController @Inject()(implicit messages: MessagesApi) extends Controller with PageHelper {

  import controllers.routes.{QuestionnaireController => routeTo}
  import views.html.{questionnaire => pages}

  private val exempt_reason_key = "exempt_reason"

  def start = Action(Ok(page(home, views.html.questionnaire.start())))

  def companyOrLLC = Action(Ok(page(home, pages.companyOrLLC())))

  def postCompanyOrLLC = Action(parse.urlFormEncoded) { implicit request =>
    val redirectTo = request.body.get("company").flatMap(_.headOption) match {
      case Some("true") => routeTo.whichFinancialYear()
      case Some("false") => routeTo.exempt
      case _ => routeTo.companyOrLLC()
    }

    Redirect(redirectTo).removingFromSession(exempt_reason_key)
  }

  def whichFinancialYear = Action(Ok(page(home, pages.whichFinancialYear())))


  def postWhichFinancialYear = Action(parse.urlFormEncoded) { implicit request =>
    val redirectTo = request.body.get("year").flatMap(_.headOption) match {
      case Some("first") => routeTo.exempt()
      case Some("second") => todo
      case Some("third") => todo
      case _ => routeTo.whichFinancialYear()
    }

    Redirect(redirectTo).addingToSession((exempt_reason_key, "reason.firstyear"))
  }

  def exempt = Action { request =>
    val reason = request.session.get(exempt_reason_key)
    Ok(page(home, pages.exempt(reason)))
  }
}
