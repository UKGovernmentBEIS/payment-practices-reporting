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

import actions.CompanyAuthRequest
import controllers.FormPageDefs._
import forms.report.ReportingPeriodFormModel
import play.api.data.Form
import play.api.mvc.{Controller, Result}
import play.api.{Logger, UnexpectedException}
import services.SessionId

import scala.concurrent.Future
import scala.util.Random

trait FormControllerHelpers[T, N <: FormName] {
  self: Controller with FormSessionHelpers =>

  implicit def sessionIdFromRequest(implicit request: CompanyAuthRequest[_]): SessionId = request.sessionId

  def formHandlers: Seq[FormHandler[_, N]]

  def bindMainForm(implicit sessionId: SessionId): Future[Option[T]]

  def bindReportingPeriod(implicit sessionId: SessionId): Future[Option[ReportingPeriodFormModel]]

  def emptyReportingPeriod: Form[ReportingPeriodFormModel]

  def handleBinding[A](request: CompanyAuthRequest[A], successHandler: (CompanyAuthRequest[A], ReportingPeriodFormModel, T) => Future[Result]): Future[Result] = {
    implicit val req: CompanyAuthRequest[A] = request

    bindAllPages[N](formHandlers).flatMap {
      case FormHasErrors(handler) => Future.successful(Redirect(handler.callPage(request.companyDetail.companiesHouseId, change = true)))
      case FormIsBlank(handler)   => Future.successful(Redirect(handler.callPage(request.companyDetail.companiesHouseId, change = true)))
      case FormIsOk(_, _)         =>
        val forms = for {
          reportingPeriod <- bindReportingPeriod
          mainForm <- bindMainForm
        } yield (reportingPeriod, mainForm)

        forms.flatMap {
          case (Some(r), Some(t)) => successHandler(request, r, t)

          // The following cases should not happen - if one of them does it indicates
          // some kind of mismatch between the FormHandlers and the base form models
          case (_, _) =>
            val ref = Random.nextInt(1000000) // for cross-referencing the error in the logs
            Logger.error(s"Error reference $ref: The reporting period and/or main form did not bind correctly")
            throw UnexpectedException(Some(s"Error reference $ref"))
        }
    }
  }
}
