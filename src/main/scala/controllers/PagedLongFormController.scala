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
import forms.report._
import models.{CompaniesHouseId, ReportId}
import play.api.data.Form
import play.api.data.Forms.{single, _}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Call, Controller, Result}
import play.twirl.api.Html
import services._
import views.html.helpers.ReviewPageData

import scala.concurrent.{ExecutionContext, Future}

class PagedLongFormController @Inject()(
                                    reports: ReportService,
                                    validations: Validations,
                                    val companyAuth: CompanyAuthService,
                                    companyAuthAction: CompanyAuthAction,
                                    val serviceConfig: ServiceConfig,
                                    val pageConfig: PageConfig,
                                    @Named("confirmation-actor") confirmationActor: ActorRef
                                  )(implicit val ec: ExecutionContext, messages: MessagesApi) extends Controller with BaseFormController with PageHelper {

  import validations._
  import views.html.{report => pages}

  private val reviewPageTitle = "Review your report"
  private def publishTitle(companyName: String) = s"Publish a report for $companyName"

  def reportPageHeader(implicit request: CompanyAuthRequest[_]): Html = h1(s"Publish a report for:<br>${request.companyDetail.companyName}")

  def postFormPage1(companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId)(parse.urlFormEncoded) { implicit request =>
    val title = publishTitle(request.companyDetail.companyName)
    val action = routes.LongFormController.postReview(companiesHouseId)

    val paymentHistory: Form[PaymentHistory] = emptyPaymentHistory.bindForm
    val reportingPeriodForm = emptyReportingPeriod.bindForm

    reportingPeriodForm.fold(
      errs => BadRequest(page(title)(home, pages.reportingPeriod(reportPageHeader, errs, paymentHistory.data, companiesHouseId, df, serviceStartDate))),
      reportingPeriod =>
        paymentHistory.fold(
          errs => BadRequest(page(title)(home, pages.longFormPage1(reportPageHeader, errs, reportingPeriodForm.data, companiesHouseId, df, serviceStartDate))),
          lf => {
            val reportData = paymentHistory.data ++ reportingPeriodForm.data
            Ok(page(reviewPageTitle)(home, pages.longFormPage2(reportPageHeader, emptyPaymentTerms, reportData, companiesHouseId, df, serviceStartDate)))
          }
        )
    )
  }

  def postFormPage2(companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId)(parse.urlFormEncoded) { implicit request =>
    ???
  }
}
