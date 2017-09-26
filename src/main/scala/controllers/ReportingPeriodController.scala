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

import javax.inject.{Inject, Named}

import actions.{CompanyAuthAction, CompanyAuthRequest}
import akka.actor.ActorRef
import config.{PageConfig, ServiceConfig}
import controllers.FormPageDefs.MultiPageFormName
import forms.report.{ReportingPeriodFormModel, Validations}
import models.CompaniesHouseId
import play.api.i18n.MessagesApi
import play.api.mvc.Controller
import play.twirl.api.Html
import services.{CompanyAuthService, ReportService, SessionId, SessionService}

import scala.concurrent.ExecutionContext

object ReportingPeriodController {
  val reportingPeriodFormId = "reporting-period-form"
}

class ReportingPeriodController @Inject()(
  reports: ReportService,
  validations: Validations,
  companyAuth: CompanyAuthService,
  companyAuthAction: CompanyAuthAction,
  val serviceConfig: ServiceConfig,
  val pageConfig: PageConfig,
  val sessionService: SessionService,
  @Named("confirmation-actor") confirmationActor: ActorRef
)(implicit val ec: ExecutionContext, messages: MessagesApi) extends Controller with PageHelper with FormSessionHelpers {

  import validations._
  import views.html.{report => pages}

  def reportPageHeader(implicit request: CompanyAuthRequest[_]): Html = h1(s"Publish a report for:<br>${request.companyDetail.companyName}")

  implicit def sessionIdFromRequest(implicit request: CompanyAuthRequest[_]): SessionId = request.sessionId

  private def publishTitle(companyName: String) = s"Publish a report for $companyName"

  private def title(implicit request: CompanyAuthRequest[_]): String = publishTitle(request.companyDetail.companyName)

  private def backCrumb(id: CompaniesHouseId, change: Option[Boolean]): Html =
    if (change.contains(true)) breadcrumbs("link-back", Breadcrumb(routes.ShortFormReviewController.showReview(id).url, "Back"))
    else breadcrumbs("link-back", Breadcrumb(routes.ReportController.start(id).url, "Back"))

  //noinspection TypeAnnotation
  def show(companiesHouseId: CompaniesHouseId, change: Option[Boolean]) = companyAuthAction(companiesHouseId).async { implicit request =>

    loadFormData(emptyReportingPeriod, MultiPageFormName.ReportingPeriod).map { form =>
      Ok(page(title)(backCrumb(companiesHouseId, change), pages.reportingPeriod(reportPageHeader, form, companiesHouseId, df, serviceStartDate, change)))
    }
  }

  //noinspection TypeAnnotation
  def post(companiesHouseId: CompaniesHouseId, change: Option[Boolean]) = companyAuthAction(companiesHouseId).async(parse.urlFormEncoded) { implicit request =>
    val reportingPeriodForm = emptyReportingPeriod.bindForm
    saveFormData(MultiPageFormName.ReportingPeriod, reportingPeriodForm).map { _ =>
      reportingPeriodForm.fold(
        errs => BadRequest(page(title)(backCrumb(companiesHouseId, change), pages.reportingPeriod(reportPageHeader, errs, companiesHouseId, df, serviceStartDate, change))),
        reportingPeriod => whereNext(companiesHouseId, change, reportingPeriod)
      )
    }
  }

  private def whereNext(companiesHouseId: CompaniesHouseId, change: Option[Boolean], reportingPeriod: ReportingPeriodFormModel) = {
    val call = (reportingPeriod.hasQualifyingContracts.toBoolean, serviceConfig.multiPageForm, change.contains(true)) match {
      case (true, true, true)  => routes.MultiPageFormReviewController.showReview(companiesHouseId)
      case (true, true, false) => routes.MultiPageFormController.show(MultiPageFormName.PaymentStatistics, companiesHouseId, change)

      case (true, false, true)  => routes.SinglePageFormReviewController.showReview(companiesHouseId)
      case (true, false, false) => routes.SinglePageFormController.show(companiesHouseId, change)

      case (false, _, true)  => routes.ShortFormReviewController.showReview(companiesHouseId)
      case (false, _, false) => routes.ShortFormController.show(companiesHouseId, change)
    }

    Redirect(call)
  }
}
