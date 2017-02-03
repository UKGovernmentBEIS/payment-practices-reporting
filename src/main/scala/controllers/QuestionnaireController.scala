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

import play.api.mvc.{Action, Controller}

class QuestionnaireController extends Controller with PageHelper {

  import controllers.routes.{QuestionnaireController => routeTo}
  import views.html.{questionnaire => pages}

  def start = Action(Ok(page(home, views.html.questionnaire.start())))

  def companyOrLLC = Action(Ok(page(home, pages.companyOrLLC())))

  def postCompanyOrLLC = Action(parse.urlFormEncoded) { implicit request =>
    val redirectTo = request.body.get("company").flatMap(_.headOption) match {
      case Some("true") => routeTo.start()
      case Some("false") => routeTo.exempt()
      case _ => routeTo.companyOrLLC()
    }

    Redirect(redirectTo)
  }

  def exempt = Action(Ok(page(pages.exempt(None))))

}
