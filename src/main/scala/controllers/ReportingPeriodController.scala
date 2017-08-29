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
import controllers.PagedLongFormModel.FormName
import forms.report.Validations
import models.CompaniesHouseId
import play.api.i18n.MessagesApi
import play.api.mvc.Controller
import play.twirl.api.Html
import services.{CompanyAuthService, ReportService, SessionService}

import scala.concurrent.ExecutionContext

class ReportingPeriodController @Inject()(
  reports: ReportService,
  validations: Validations,
  pagedLongFormData: PagedLongFormData,
  companyAuth: CompanyAuthService,
  companyAuthAction: CompanyAuthAction,
  val serviceConfig: ServiceConfig,
  val pageConfig: PageConfig,
  val sessionService: SessionService,
  pagedLongFormModel: PagedLongFormModel,
  @Named("confirmation-actor") confirmationActor: ActorRef
)(implicit val ec: ExecutionContext, messages: MessagesApi) extends Controller with PageHelper with FormSessionHelpers {

  import validations._
  import views.html.{report => pages}

  def reportPageHeader(implicit request: CompanyAuthRequest[_]): Html = h1(s"Publish a report for:<br>${request.companyDetail.companyName}")

  private def publishTitle(companyName: String) = s"Publish a report for $companyName"

  private def title(implicit request: CompanyAuthRequest[_]): String = publishTitle(request.companyDetail.companyName)

  def show(companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId).async { implicit request =>
    loadFormData(emptyReportingPeriod, FormName.PaymentStatistics.entryName).map { form =>
      Ok(page(title)(home, pages.reportingPeriod(reportPageHeader, form, companiesHouseId, df, serviceStartDate)))
    }
  }

  def post(companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId).async(parse.urlFormEncoded) { implicit request =>
    val reportingPeriodForm = emptyReportingPeriod.bindForm
    saveFormData(FormName.ReportingPeriod, reportingPeriodForm).map { _ =>
      reportingPeriodForm.fold(
        errs => BadRequest(page(title)(home, pages.reportingPeriod(reportPageHeader, errs, companiesHouseId, df, serviceStartDate))),
        reportingPeriod =>
          if (reportingPeriod.hasQualifyingContracts.toBoolean)
            Redirect(routes.PagedLongFormController.show(FormName.PaymentStatistics, companiesHouseId))
          else
            Redirect(routes.ShortFormController.show(companiesHouseId))
      )
    }
  }
}
