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

import config.GoogleAnalyticsConfig
import models.{DecisionState, Question}
import org.scalactic.TripleEquals._
import play.api.data.{Form, FormError}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}
import questionnaire._

class QuestionnaireController @Inject()(summarizer: Summarizer, val googleAnalytics: GoogleAnalyticsConfig)(implicit messages: MessagesApi) extends Controller with PageHelper {

  import QuestionnaireValidations._
  import views.html.{questionnaire => pages}

  def start = Action { implicit request =>
    Ok(page("Find out if your business needs to publish reports")(home, views.html.questionnaire.start()))
  }

  val emptyForm = Form(decisionStateMapping)

  def nextQuestion = Action { implicit request =>
    val form = emptyForm.bindFromRequest
    val currentState = form.fold(_ => DecisionState.empty, s => s)

    // By filling the empty form from the current state we ensure that we filter
    // out any submitted values (such as the submit button) from the data we
    // are passing into the next question form. We don't want those other values
    // being rendered as hidden fields.
    val formData = emptyForm.fill(currentState).data

    val exemptTitle = "Your business does not need to publish reports"

    Decider.calculateDecision(currentState) match {
      case AskQuestion(q) =>
        Ok(page(messages(q.textKey))(home, pages.question(q, formData, questionError(q, form("question-key").value))))
      case NotACompany(reason) => Ok(page(exemptTitle)(home, pages.notACompany(reason)))
      case Exempt(reason) => Ok(page(exemptTitle)(home, pages.exempt(reason)))
      case Required => Ok(page("Your business must publish reports")(home, pages.required(summarizer.summarize(currentState))))
    }
  }

  /**
    * Is the question to ask next the same as the one we just asked? If so, this indicates the form
    * was posted without an answer (i.e. the user did not select a choice), in which case we should
    * show an error message when we re-display the form.
    */
  private def questionError(q: Question, questionKey: Option[String]): Option[FormError] = questionKey.flatMap {
    case k if k === q.fieldKey => Some(FormError(k, "error.needchoicetocontinue"))
    case _ => None
  }
}
