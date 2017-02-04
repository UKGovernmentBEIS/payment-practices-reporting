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

import play.api.data.Forms._
import play.api.data.Mapping
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}
import questionnaire.{DecisionState, FinancialYear, Thresholds, YesNo}

class QuestionnaireController @Inject()(implicit messages: MessagesApi) extends Controller with PageHelper {

  val yesNo: Mapping[YesNo] = ???
  val financialYear: Mapping[FinancialYear] = ???
  val thresholds: Mapping[Thresholds] = mapping(
    "turnover" -> optional(yesNo),
    "balanceSheet" -> optional(yesNo),
    "employees" -> optional(yesNo)
  )(Thresholds.apply)(Thresholds.unapply)

  val decisionStateMapping: Mapping[DecisionState] = mapping(
    "isCompanyOrLLP" -> optional(yesNo),
    "financialYear" -> optional(financialYear),
    "companyThresholds" -> thresholds,
    "subsidiaries" -> optional(yesNo),
    "subsidiaryThresholds" -> thresholds
  )(DecisionState.apply)(DecisionState.unapply)

  val emptyState = decisionStateMapping.unbind(DecisionState.empty)

  import controllers.routes.{QuestionnaireController => routeTo}
  import views.html.{questionnaire => pages}

  private val exemptReasonKey = "exempt_reason"

  def start = Action { implicit request =>
    Ok(page(home, views.html.questionnaire.start())).addingToSession(emptyState.toSeq: _*)
  }

  def companyOrLLC = Action(Ok(page(home, pages.companyOrLLC())))

  def postCompanyOrLLC = Action(parse.urlFormEncoded) { implicit request =>
    val redirectTo = request.body.get("company").flatMap(_.headOption) match {
      case Some("true") => routeTo.whichFinancialYear()
      case Some("false") => routeTo.exempt()
      case _ => routeTo.companyOrLLC()
    }

    Redirect(redirectTo).removingFromSession(exemptReasonKey)
  }

  def whichFinancialYear = Action(Ok(page(home, pages.whichFinancialYear())))


  def postWhichFinancialYear = Action(parse.urlFormEncoded) { implicit request =>
    val redirectTo = request.body.get("year").flatMap(_.headOption) match {
      case Some(name) if name == FinancialYear.First.entryName => routeTo.exempt()
      case Some(name) if name == FinancialYear.Second.entryName => todo
      case Some(name) if name == FinancialYear.ThirdOrLater.entryName => todo
      case _ => routeTo.whichFinancialYear()
    }

    Redirect(redirectTo).addingToSession((exemptReasonKey, "reason.firstyear"))
  }

  def exempt = Action { request =>
    val reason = request.session.get(exemptReasonKey)
    Ok(page(home, pages.exempt(reason)))
  }
}
