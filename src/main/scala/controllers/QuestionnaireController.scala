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

import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller, Request}
import questionnaire._

class QuestionnaireController @Inject()(decider: Decider)(implicit messages: MessagesApi) extends Controller with PageHelper {

  import QuestionnaireValidations._
  import controllers.routes.{QuestionnaireController => routeTo}
  import views.html.{questionnaire => pages}

  private val exemptReasonKey = "exempt_reason"

  def start = Action { implicit request =>
    val cleanState = request.session.data.filterNot(_._1.startsWith("ds."))
    Ok(page(home, views.html.questionnaire.start())).withSession(cleanState.toSeq: _*)
  }

  private def sessionState(implicit request: Request[_]): Map[String, String] =
    request.session.data.filter(_._1.startsWith("ds."))

  def nextQuestion = Action { implicit request =>
    val state = stateHolderMapping.bind(sessionState).fold(_ => DecisionState.empty, s => s.decisionState)

    Logger.debug(state.toString)

    decider.calculateDecision(state) match {
      case aq@AskQuestion(key, q) => Ok(page(home, pages.question(key, q)))
      case Exempt(Some(reason)) => Redirect(routeTo.exempt()).addingToSession((exemptReasonKey, reason))
      case Exempt(None) => Redirect(routeTo.exempt()).removingFromSession(exemptReasonKey)
      case Required => Redirect(routeTo.required())
    }
  }

  private def stateValues(input: Map[String, Seq[String]]): Map[String, String] = input.map { case (k, v) =>
    (k, v.headOption)
  }.collect {
    case (k, Some(v)) if k.startsWith("ds.") => (k, v)
  }

  def postAnswer = Action(parse.urlFormEncoded) { implicit request =>
    val priorState = sessionState
    val formState = stateValues(request.body)
    val combinedState = priorState ++ formState

    Logger.debug(priorState.toString)
    Logger.debug(formState.toString)
    Logger.debug(combinedState.toString)

    stateHolderMapping.bind(combinedState).fold(
      errs => Redirect(routeTo.nextQuestion()),
      newState => Redirect(routeTo.nextQuestion()).addingToSession(stateHolderMapping.unbind(newState).toSeq: _*)
    )
  }

  def exempt = Action { request =>
    val reason = request.session.get(exemptReasonKey)
    Ok(page(home, pages.exempt(reason)))
  }

  def required = Action { implicit request =>
    Ok(page(home, pages.required()))
  }
}
