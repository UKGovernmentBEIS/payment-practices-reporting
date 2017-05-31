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

import scala.concurrent.{ExecutionContext, Future}

class FilingController @Inject()(
                                  reports: ReportService,
                                  validations: Validations,
                                  companyAuth: CompanyAuthService,
                                  CompanyAuthAction: CompanyAuthAction,
                                  val serviceConfig: ServiceConfig,
                                  val pageConfig: PageConfig,
                                  @Named("confirmation-actor") confirmationActor: ActorRef
                                )(implicit ec: ExecutionContext, messages: MessagesApi) extends Controller with PageHelper {

  import views.html.{report => pages}

  val emptyReportingPeriod: Form[ReportingPeriodFormModel] = Form(validations.reportingPeriodFormModel)
  val emptyLongForm: Form[LongFormModel] = Form(validations.reportFormModel)
  val emptyPaymentCodes: Form[PaymentCodesFormModel] = Form(validations.paymentCodesFormModel)
  val emptyReview: Form[ReportReviewModel] = Form(validations.reportReviewModel)
  private val reviewPageTitle = "Review your report"
  private def publishTitle(companyName: String) = s"Publish a report for $companyName"

  def reportPageHeader(implicit request: CompanyAuthRequest[_]): Html = h1(s"Publish a report for:<br>${request.companyDetail.companyName}")

  def postShortForm(companiesHouseId: CompaniesHouseId) = CompanyAuthAction(companiesHouseId)(parse.urlFormEncoded) { implicit request =>
    val title = publishTitle(request.companyDetail.companyName)
    val action = routes.FilingController.postShortFormReview(companiesHouseId)

    val paymentCodesForm = emptyPaymentCodes.bindForm
    val reportingPeriodForm = emptyReportingPeriod.bindForm

    reportingPeriodForm.fold(
      errs => BadRequest(page(title)(home, pages.reportingPeriod(reportPageHeader, errs, emptyLongForm, paymentCodesForm, companiesHouseId, df, serviceStartDate))),
      reportingPeriod => paymentCodesForm.fold(
        errs => BadRequest(page(title)(home, pages.longForm(reportPageHeader, emptyLongForm, errs, reportingPeriod, companiesHouseId, df, serviceStartDate, validations))),
        paymentCodes => Ok(page(reviewPageTitle)(home,
          pages.review(emptyReview, None, paymentCodes, reportingPeriod, action, companiesHouseId, request.companyDetail.companyName, df, validations)
        ))
      )
    )
  }

  def postLongForm(companiesHouseId: CompaniesHouseId) = CompanyAuthAction(companiesHouseId)(parse.urlFormEncoded) { implicit request =>
    val title = publishTitle(request.companyDetail.companyName)
    val action = routes.FilingController.postLongFormReview(companiesHouseId)

    val longForm = emptyLongForm.bindForm
    val paymentCodesForm = emptyPaymentCodes.bindForm
    val reportingPeriodForm = emptyReportingPeriod.bindForm

    reportingPeriodForm.fold(
      errs => BadRequest(page(title)(home, pages.reportingPeriod(reportPageHeader, errs, longForm, paymentCodesForm, companiesHouseId, df, serviceStartDate))),
      reportingPeriod => longForm.fold(
        errs => BadRequest(page(title)(home, pages.longForm(reportPageHeader, errs, paymentCodesForm, reportingPeriod, companiesHouseId, df, serviceStartDate, validations))),
        longForm => Ok(page(reviewPageTitle)(home,
          pages.review(emptyReview, Some(longForm), paymentCodesForm.get, reportingPeriod, action, companiesHouseId, request.companyDetail.companyName, df, validations)
        ))
      )
    )
  }

  def postShortFormReview(companiesHouseId: CompaniesHouseId) = CompanyAuthAction(companiesHouseId).async(parse.urlFormEncoded) { implicit request =>
    val revise: Boolean = Form(single("revise" -> text)).bindForm.value.contains("Revise")
    val title = publishTitle(request.companyDetail.companyName)

    val paymentCodesForm = emptyPaymentCodes.bindForm
    val reportingPeriodForm = emptyReportingPeriod.bindForm

    reportingPeriodForm.fold(
      errs => Future.successful(BadRequest(page(title)(home, pages.reportingPeriod(reportPageHeader, errs, emptyLongForm, paymentCodesForm, companiesHouseId, df, serviceStartDate)))),
      reportingPeriod => paymentCodesForm.fold(
        errs => Future.successful(BadRequest(page(title)(home, pages.shortForm(reportPageHeader, errs, reportingPeriod, companiesHouseId, df, serviceStartDate, validations)))),
        rp =>
          if (revise) Future.successful(Ok(page(title)(home, pages.reportingPeriod(reportPageHeader, reportingPeriodForm, emptyLongForm, paymentCodesForm, companiesHouseId, df, serviceStartDate))))
          else checkConfirmation(companiesHouseId, reportingPeriod, None, paymentCodesForm.get)
      )
    )
  }

  def postLongFormReview(companiesHouseId: CompaniesHouseId) = CompanyAuthAction(companiesHouseId).async(parse.urlFormEncoded) { implicit request =>
    val revise: Boolean = Form(single("revise" -> text)).bindForm.value.contains("Revise")
    val title = publishTitle(request.companyDetail.companyName)

    // Re-capture the values for the report itself. In theory these values should always be valid
    // (as we only send the user to the review page if they are) but if somehow they aren't then
    // send the user back to the report form to fix them.
    val longForm = emptyLongForm.bindForm
    val paymentCodesForm = emptyPaymentCodes.bindForm
    val reportingPeriodForm = emptyReportingPeriod.bindForm

    reportingPeriodForm.fold(
      errs => Future.successful(BadRequest(page(title)(home, pages.reportingPeriod(reportPageHeader, errs, longForm, paymentCodesForm, companiesHouseId, df, serviceStartDate)))),
      reportingPeriod => longForm.fold(
        errs => Future.successful(BadRequest(page(title)(home, pages.longForm(reportPageHeader, errs, paymentCodesForm, reportingPeriod, companiesHouseId, df, serviceStartDate, validations)))),
        lf =>
          if (revise) Future.successful(Ok(page(title)(home, pages.reportingPeriod(reportPageHeader, reportingPeriodForm, longForm, paymentCodesForm, companiesHouseId, df, serviceStartDate))))
          else checkConfirmation(companiesHouseId, reportingPeriod, Some(lf), paymentCodesForm.get)
      )
    )
  }

  private def checkConfirmation(companiesHouseId: CompaniesHouseId, reportingPeriod: ReportingPeriodFormModel, longForm: Option[LongFormModel], paymentCodesForm: PaymentCodesFormModel)(implicit request: CompanyAuthRequest[Map[String, Seq[String]]]): Future[Result] = {
    val action: Call = longForm.map(_ => routes.FilingController.postLongFormReview(companiesHouseId)).getOrElse(routes.FilingController.postShortFormReview(companiesHouseId))
    emptyReview.bindForm.fold(
      errs => Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(errs, longForm, paymentCodesForm, reportingPeriod, action, companiesHouseId, request.companyDetail.companyName, df, validations)))),
      review => {
        if (review.confirmed) verifyingOAuthScope(companiesHouseId, request.oAuthToken) {
          createReport(companiesHouseId, reportingPeriod, longForm, paymentCodesForm, review).map(rId => Redirect(controllers.routes.FilingController.showConfirmation(rId)))
        }
        else
          Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(emptyReview.fill(review), longForm, paymentCodesForm, reportingPeriod, action, companiesHouseId, request.companyDetail.companyName, df, validations))))
      }
    )
  }

  private def verifyingOAuthScope(companiesHouseId: CompaniesHouseId, oAuthToken: OAuthToken)(body: => Future[Result]): Future[Result] = {
    companyAuth.isInScope(companiesHouseId, oAuthToken).flatMap {
      case true => body
      case false => Future.successful(Redirect(controllers.routes.FilingController.invalidScope(companiesHouseId)))
    }
  }

  private def createReport(companiesHouseId: CompaniesHouseId, reportingPeriod: ReportingPeriodFormModel, longForm: Option[LongFormModel], paymentCodesForm: PaymentCodesFormModel, review: ReportReviewModel)(implicit request: CompanyAuthRequest[_]): Future[ReportId] = {

    val urlFunction: ReportId => String = (id: ReportId) => controllers.routes.ReportController.view(id).absoluteURL()
    for {
      reportId <- reports.create(request.companyDetail, reportingPeriod, longForm, paymentCodesForm, review, request.emailAddress, urlFunction)
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

  def invalidScope(companiesHouseId: CompaniesHouseId) = CompanyAuthAction(companiesHouseId) { implicit request =>
    Ok(page("Your report has not been filed because of an error")(home, pages.invalidScope(request.companyDetail)))
  }
}
