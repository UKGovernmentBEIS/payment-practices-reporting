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

class LongFormController @Inject()(
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

  def postForm(companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId)(parse.urlFormEncoded) { implicit request =>
    val title = publishTitle(request.companyDetail.companyName)
    val action = routes.LongFormController.postReview(companiesHouseId)

    val longForm: Form[LongFormModel] = emptyLongForm.bindForm
    val reportingPeriodForm = emptyReportingPeriod.bindForm

    reportingPeriodForm.fold(
      errs => BadRequest(page(title)(home, pages.reportingPeriod(reportPageHeader, errs, longForm.data, companiesHouseId, df, serviceStartDate))),
      reportingPeriod =>
        longForm.fold(
          errs => BadRequest(page(title)(home, pages.longForm(reportPageHeader, errs, reportingPeriodForm.data, companiesHouseId, df, serviceStartDate))),
          lf => {
            val reportData = longForm.data ++ reportingPeriodForm.data
            val formGroups = ReviewPageData.formGroups(request.companyDetail.companyName, reportingPeriod, lf)
            Ok(page(reviewPageTitle)(home, pages.review(emptyReview, reportData, formGroups, action)))
          }
        )
    )
  }

  def postReview(companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId).async(parse.urlFormEncoded) { implicit request =>
    val revise: Boolean = Form(single("revise" -> text)).bindForm.value.contains("Revise")
    val title = publishTitle(request.companyDetail.companyName)

    // Re-capture the values for the report itself. In theory these values should always be valid
    // (as we only send the user to the review page if they are) but if somehow they aren't then
    // send the user back to the report form to fix them.
    val longForm = emptyLongForm.bindForm
    val reportingPeriodForm = emptyReportingPeriod.bindForm

    reportingPeriodForm.fold(
      errs => Future.successful(BadRequest(page(title)(home, pages.reportingPeriod(reportPageHeader, errs, longForm.data, companiesHouseId, df, serviceStartDate)))),
      reportingPeriod => longForm.fold(
        errs => Future.successful(BadRequest(page(title)(home, pages.longForm(reportPageHeader, errs, reportingPeriodForm.data, companiesHouseId, df, serviceStartDate)))),
        lf =>
          if (revise) Future.successful(Ok(page(title)(home, pages.reportingPeriod(reportPageHeader, reportingPeriodForm, longForm.data, companiesHouseId, df, serviceStartDate))))
          else checkConfirmation(companiesHouseId, reportingPeriod, lf)
      )
    )
  }

  private def checkConfirmation(companiesHouseId: CompaniesHouseId, reportingPeriod: ReportingPeriodFormModel, longForm: LongFormModel)(implicit request: CompanyAuthRequest[Map[String, Seq[String]]]): Future[Result] = {
    val action: Call = routes.LongFormController.postReview(companiesHouseId)
    val companyName: String = request.companyDetail.companyName

    val reportData = emptyReportingPeriod.fill(reportingPeriod).data ++ emptyLongForm.fill(longForm).data
    val formGroups = ReviewPageData.formGroups(companyName, reportingPeriod, longForm)

    emptyReview.bindForm.fold(
      errs => Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(errs, reportData, formGroups, action)))),
      review => {
        if (review.confirmed) verifyingOAuthScope(companiesHouseId, request.oAuthToken) {
          createReport(companiesHouseId, reportingPeriod, longForm, review).map(rId => Redirect(controllers.routes.ConfirmationController.showConfirmation(rId)))
        } else {
          Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(emptyReview.fill(review), reportData, formGroups, action))))
        }
      }
    )
  }

  private def createReport(companiesHouseId: CompaniesHouseId, reportingPeriod: ReportingPeriodFormModel, longForm: LongFormModel, review: ReportReviewModel)(implicit request: CompanyAuthRequest[_]): Future[ReportId] = {
    val urlFunction: ReportId => String = (id: ReportId) => controllers.routes.ReportController.view(id).absoluteURL()
    for {
      reportId <- reports.create(request.companyDetail, reportingPeriod, longForm, review, request.emailAddress, urlFunction)
      _ <- Future.successful(confirmationActor ! 'poll)
    } yield reportId
  }

  def showConfirmation(reportId: ReportId) = Action.async { implicit request =>
    reports.find(reportId).map {
      case Some(report) => Ok(
        page(s"You have published a report for ${report.companyName}")
        (home, pages.filingSuccess(reportId, report.confirmationEmailAddress, pageConfig.surveyMonkeyConfig)))
      case None => BadRequest(s"Could not find a report with id ${reportId.id}")
    }
  }
}
