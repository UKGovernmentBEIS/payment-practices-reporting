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
import forms.report.Validations
import models.CompaniesHouseId
import play.api.i18n.MessagesApi
import play.api.mvc.Controller
import play.twirl.api.Html
import services.{CompanyAuthService, ReportService}

import scala.concurrent.ExecutionContext

class ReportingPeriodController @Inject()(
                                           reports: ReportService,
                                           validations: Validations,
                                           companyAuth: CompanyAuthService,
                                           companyAuthAction: CompanyAuthAction,
                                           val serviceConfig: ServiceConfig,
                                           val pageConfig: PageConfig,
                                           @Named("confirmation-actor") confirmationActor: ActorRef
                                         )(implicit ec: ExecutionContext, messages: MessagesApi) extends Controller with PageHelper {

  import validations._
  import views.html.{report => pages}

  def reportPageHeader(implicit request: CompanyAuthRequest[_]): Html = h1(s"Publish a report for:<br>${request.companyDetail.companyName}")
  private def publishTitle(companyName: String) = s"Publish a report for $companyName"
  private def title(implicit request: CompanyAuthRequest[_]): String = publishTitle(request.companyDetail.companyName)

  def startReport(companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId) { implicit request =>
    Ok(page(title)(home, pages.reportingPeriod(reportPageHeader, emptyReportingPeriod, Map.empty, companiesHouseId, df, serviceStartDate)))
  }

  def post(companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId)(parse.urlFormEncoded) { implicit request =>
    // Catch any stashed values for the main report (if we came back from the review page) but
    // we don't want to carry any errors forward when we progress to the next page. This also
    // makes sure that when the user starts filing a new report that the next page doesn't start
    // out full of errors because the report is empty.
    val longForm = emptyLongForm.bindForm.discardingErrors
    val shortForm = emptyShortForm.bindForm.discardingErrors

    // The longForm is a superset of the shortForm so its data will include all the fields we
    // need to stash if we go back to the reportingPeriod page
    val stashData: Map[String, String] = longForm.data

    val reportingPeriodForm = emptyReportingPeriod.bindForm
    reportingPeriodForm.fold(
    errs => BadRequest(page(title)(home, pages.reportingPeriod(reportPageHeader, errs, stashData, companiesHouseId, df, serviceStartDate))),
       reportingPeriod =>
        if (reportingPeriod.hasQualifyingContracts.toBoolean)
          Ok(page(title)(home, pages.longForm(reportPageHeader, longForm, reportingPeriodForm.data, companiesHouseId, df, serviceStartDate)))
        else
          Ok(page(title)(home, pages.shortForm(reportPageHeader, shortForm, reportingPeriodForm.data, companiesHouseId, df, serviceStartDate)))

    )
  }
}
