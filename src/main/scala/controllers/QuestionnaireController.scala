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

import config.{PageConfig, ServiceConfig}
import org.scalactic.TripleEquals._
import play.api.data.{Form, FormError}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}
import questionnaire.{DecisionState, _}

object QuestionnaireController {
  val startTitle              = "Find out if your business needs to publish reports"
  val startButtonId           = "start-button"
  val exemptTitle             = "Your business does not need to publish reports"
  val mustReportTitle         = "Your business must publish reports"
  val companyReasonsListId    = "company-reasons"
  val subsidiaryReasonsListId = "subsidiary-reasons"
}

class QuestionnaireController @Inject()(
  summarizer: Summarizer,
  val pageConfig: PageConfig,
  val serviceConfig: ServiceConfig
)
  (implicit messages: MessagesApi) extends Controller with PageHelper {

  import QuestionnaireController._
  import QuestionnaireValidations._
  import views.html.{questionnaire => pages}

  def start = Action { implicit request =>
    val externalRouter = implicitly[ExternalRouter]
    Ok(page(startTitle)(home, views.html.questionnaire.start(externalRouter)))
  }

  val emptyForm = Form(decisionStateMapping)

  def firstQuestion = Action { implicit request =>
    val formData = emptyForm.fill(DecisionState.empty).data
    val q = Questions.isCompanyOrLLPQuestion
    Ok(page(messages(q.textKey))(home, pages.question(q, formData, None)))
  }

  def nextQuestion: Action[Map[String, Seq[String]]] = Action(parse.urlFormEncoded) { implicit request =>
    val form = emptyForm.bindFromRequest
    val currentState = form.fold(_ => DecisionState.empty, s => s)

    // By filling the empty form from the current state we ensure that we filter
    // out any submitted values (such as the submit button) from the data we
    // are passing into the next question form. We don't want those other values
    // being rendered as hidden fields.
    val formData = emptyForm.fill(currentState).data

    Decider.calculateDecision(currentState) match {
      case Left(q) =>
        questionError(q, form("question-key").value) match {
          case None        => Ok(page(messages(q.textKey))(home, pages.question(q, formData, None)))
          case Some(error) => BadRequest(page(messages(q.textKey))(home, pages.question(q, formData, Some(error))))
        }

      case Right(NotACompany(reason)) => Ok(page(exemptTitle)(home, pages.notACompany(reason)))
      case Right(Exempt(reason))      => Ok(page(exemptTitle)(home, pages.exempt(reason)))
      case Right(Required)            => Ok(page(mustReportTitle)(home, pages.required(summarizer.summarize(currentState))))
    }
  }

  /**
    * Is the question to ask next the same as the one we just asked? If so, this indicates the form
    * was posted without an answer (i.e. the user did not select a choice), in which case we should
    * show an error message when we re-display the form.
    */
  private def questionError(q: Question, questionKey: Option[String]): Option[FormError] = questionKey.flatMap {
    case k if k === q.fieldKey => Some(FormError(k, "error.needchoicetocontinue"))
    case _                     => None
  }
}
