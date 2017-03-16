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
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}
import questionnaire._

class QuestionnaireController @Inject()(summarizer: Summarizer, val appConfig: AppConfig)(implicit messages: MessagesApi) extends Controller with PageHelper {

  import QuestionnaireValidations._
  import controllers.routes.{QuestionnaireController => routeTo}
  import views.html.{questionnaire => pages}

  private val exemptReasonKey = "exempt_reason"

  def start = Action { implicit request =>
    Ok(page("Find out if your business needs to publish reports")(home, views.html.questionnaire.start()))
      .removing(decisionStateMapping)
      .removingFromSession(exemptReasonKey)
  }

  def startQuestions = Action { implicit request =>
    Redirect(controllers.routes.QuestionnaireController.nextQuestion())
      .removing(decisionStateMapping)
      .removingFromSession(exemptReasonKey)
  }

  def nextQuestion = Action { implicit request =>
    val state = decisionStateMapping.bindFromRequest.fold(_ => DecisionState.empty, s => s)

    Decider.calculateDecision(state) match {
      case AskQuestion(q) => Ok(page(messages(q.textKey))(home, pages.question(q)))
      case Exempt(Some(reason)) => Redirect(routeTo.exempt()).addingToSession((exemptReasonKey, reason))
      case Exempt(None) => Redirect(routeTo.exempt()).removingFromSession(exemptReasonKey)
      case Required => Redirect(routeTo.required())
    }
  }

  def postAnswer = Action(parse.urlFormEncoded) { implicit request =>
    val priorState = decisionStateMapping.sessionState
    val formState = decisionStateMapping.stateValues(request.body)
    val combinedState = priorState ++ formState

    decisionStateMapping.bind(combinedState).fold(
      _ => Redirect(routeTo.nextQuestion()), // try again without modifying the session state
      newState => Redirect(routeTo.nextQuestion()).addingToSession(decisionStateMapping.unbind(newState).toSeq: _*)
    )
  }

  def exempt = Action(request => Ok(page("Your business does not need to publish reports")(home, pages.exempt(request.session.get(exemptReasonKey)))))

  def required = Action { implicit request =>
    val state = decisionStateMapping.bindFromRequest.fold(_ => DecisionState.empty, s => s)

    val summary = summarizer.summarize(state)
    Ok(page("Your business must publish reports")(home, pages.required(summary)))
  }
}
