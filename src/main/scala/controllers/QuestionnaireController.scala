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
import play.api.mvc.{Action, Controller, Request}
import questionnaire._

class QuestionnaireController @Inject()(decider: Decider)(implicit messages: MessagesApi) extends Controller with PageHelper {

  import QuestionnaireValidations._
  import controllers.routes.{QuestionnaireController => routeTo}
  import views.html.{questionnaire => pages}

  private val exemptReasonKey = "exempt_reason"

  private val stateWrapperPrefix = "ds."

  def start = Action { implicit request =>
    val cleanState = request.session.data.filterNot(_._1.startsWith(stateWrapperPrefix))

    Ok(page(home, views.html.questionnaire.start()))
      .withSession(cleanState.toSeq: _*)
      .removingFromSession(exemptReasonKey)
  }

  private def sessionState(implicit request: Request[_]): Map[String, String] =
    request.session.data.filter(_._1.startsWith(stateWrapperPrefix))

  def nextQuestion = Action { implicit request =>
    val state = stateHolderMapping.bind(sessionState).fold(_ => DecisionState.empty, s => s.decisionState)

    decider.calculateDecision(state) match {
      case AskQuestion(key, q) => Ok(page(home, pages.question(key, q)))
      case Exempt(Some(reason)) => Redirect(routeTo.exempt()).addingToSession((exemptReasonKey, reason))
      case Exempt(None) => Redirect(routeTo.exempt()).removingFromSession(exemptReasonKey)
      case Required => Redirect(routeTo.required())
    }
  }

  /**
    * The url-encoded form values arrive in the form of a `Map[String, Seq[String]]`. For convenience,
    * reduce this to a `Map[String, String]` (as we only expect one value per form key) and filter out
    * keys that don't start with the state wrapper prefix to eliminate noise.
    */
  private def stateValues(input: Map[String, Seq[String]]): Map[String, String] =
    input.map { case (k, v) => (k, v.headOption) }
      .collect { case (k, Some(v)) if k.startsWith(stateWrapperPrefix) => (k, v) }

  def postAnswer = Action(parse.urlFormEncoded) { implicit request =>
    val priorState = sessionState
    val formState = stateValues(request.body)
    val combinedState = priorState ++ formState

    stateHolderMapping.bind(combinedState).fold(
      _ => Redirect(routeTo.nextQuestion()), // try again without modifying the session state
      newState => Redirect(routeTo.nextQuestion()).addingToSession(stateHolderMapping.unbind(newState).toSeq: _*)
    )
  }

  def exempt = Action(request => Ok(page(home, pages.exempt(request.session.get(exemptReasonKey)))))

  def required = Action(Ok(page(home, pages.required())))
}
