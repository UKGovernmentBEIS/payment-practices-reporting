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

import config.AppConfig
import models.DecisionState
import org.scalactic.TripleEquals._
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}
import questionnaire._

class QuestionnaireController @Inject()(summarizer: Summarizer, val appConfig: AppConfig)(implicit messages: MessagesApi) extends Controller with PageHelper {

  import QuestionnaireValidations._
  import views.html.{questionnaire => pages}

  def start = Action { implicit request =>
    Ok(page("Find out if your business needs to publish reports")(home, views.html.questionnaire.start()))
  }

  def nextQuestion = Action { implicit request =>
    val form = Form(decisionStateMapping).bindFromRequest
    val currentState = form.fold(_ => DecisionState.empty, s => s)
    // The form will bind the "Continue" button as a value from the request so
    // filter it out of the data we render as hidden fields on the form.
    val formData = form.data.filter { case (k, _) => k !== "Continue" }

    Decider.calculateDecision(currentState) match {
      case AskQuestion(q) => Ok(page(messages(q.textKey))(home, pages.question(q, formData)))
      case NotACompany => Ok(page("Your business does not need to publish reports")(home, pages.notACompany("reason.notacompany")))
      case Exempt(reason) => Ok(page("Your business does not need to publish reports")(home, pages.exempt(reason)))
      case Required => Ok(page("Your business must publish reports")(home, pages.required(summarizer.summarize(currentState))))
    }
  }
}
