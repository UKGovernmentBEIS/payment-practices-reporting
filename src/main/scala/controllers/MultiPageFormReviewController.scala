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
import controllers.FormPageDefs.MultiPageFormName._
import controllers.FormPageDefs._
import forms.report.{LongFormModel, ReportingPeriodFormModel, Validations}
import models.{CompaniesHouseId, ReportId}
import play.api.data.Form
import play.api.data.Forms.{single, text}
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc._
import play.twirl.api.Html
import services._
import views.html.helpers.ReviewPageData

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{existentials, implicitConversions}

class MultiPageFormReviewController @Inject()(
  reports: ReportService,
  validations: Validations,
  val companyAuth: CompanyAuthService,
  companyAuthAction: CompanyAuthAction,
  val serviceConfig: ServiceConfig,
  val pageConfig: PageConfig,
  val sessionService: SessionService,
  longFormPageModel: MultiPageFormPageModel,
  reviewPageData: ReviewPageData,
  @Named("confirmation-actor") confirmationActor: ActorRef
)(implicit val ec: ExecutionContext, messages: MessagesApi)
  extends Controller
    with BaseFormController
    with PageHelper
    with FormSessionHelpers
    with FormControllerHelpers[LongFormModel, MultiPageFormName] {

  import longFormPageModel._
  import validations._
  import views.html.{report => pages}

  private val reviewPageTitle = "Review your report"

  private def publishTitle(companyName: String) = s"Publish a report for $companyName"

  implicit def companyDetail(implicit request: CompanyAuthRequest[_]): CompanyDetail = request.companyDetail

  def reportPageHeader(companyDetail: CompanyDetail): Html = h1(s"Publish a report for:<br>${companyDetail.companyName}")

  override def formHandlers: Seq[MultiPageFormHandler[_]] = longFormPageModel.formHandlers

  override val emptyReportingPeriod: Form[ReportingPeriodFormModel] = validations.emptyReportingPeriod

  override def bindReportingPeriod(implicit sessionId: SessionId): Future[Option[ReportingPeriodFormModel]] =
    loadFormData(emptyReportingPeriod, MultiPageFormName.ReportingPeriod).map(_.value)

  override def bindMainForm(implicit sessionId: SessionId): Future[Option[LongFormModel]] =
    loadAllFormData.map { data =>
      for {
        ps <- emptyPaymentStatisticsForm.bind((data \\ PaymentStatistics.entryName).headOption.getOrElse(Json.obj())).value
        pt <- emptyPaymentTermsForm.bind((data \\ PaymentTerms.entryName).headOption.getOrElse(Json.obj())).value
        dr <- emptyDisputeResolutionForm.bind((data \\ DisputeResolution.entryName).headOption.getOrElse(Json.obj())).value
        oi <- emptyOtherInformationForm.bind((data \\ OtherInformation.entryName).headOption.getOrElse(Json.obj())).value
      } yield LongFormModel(ps.paymentStatistics, pt.paymentTerms, dr.disputeResolution, oi.otherInformation)
    }

  def showReview(companiesHouseId: CompaniesHouseId): Action[AnyContent] = companyAuthAction(companiesHouseId).async { implicit request =>
    handleBinding(request, renderReview)
  }

  //noinspection TypeAnnotation
  def postReview(companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId).async(parse.urlFormEncoded) { implicit request =>
    val revise: Boolean = Form(single("revise" -> text)).bindForm.value.contains("Revise")

    if (revise) Future.successful(Redirect(routes.ReportingPeriodController.show(companiesHouseId, None)))
    else handleBinding(request, handleReviewPost)
  }

  private def renderReview(request: CompanyAuthRequest[_], r: ReportingPeriodFormModel, lf: LongFormModel): Future[Result] = {
    implicit val req: CompanyAuthRequest[_] = request
    val back = longFormPageModel.formHandlers.lastOption match {
      case Some(handler) => backCrumb(handler.callPage(request.companyDetail.companiesHouseId, change = false).url)
      // The form handlers list should never be empty so we should ever hit this case, but just to be sure...
      case None => backCrumb(routes.ReportController.search(None, None, None).url)
    }
    val title = publishTitle(request.companyDetail.companyName)
    val action: Call = routes.MultiPageFormReviewController.postReview(request.companyDetail.companiesHouseId)
    val formGroups = reviewPageData.formGroups(r, lf)
    Future.successful(Ok(page(title)(back, views.html.report.review(emptyReview, formGroups, action))))
  }

  private def handleReviewPost(request: CompanyAuthRequest[Map[String, Seq[String]]], reportingPeriod: ReportingPeriodFormModel, longForm: LongFormModel): Future[Result] = {
    implicit val req: CompanyAuthRequest[Map[String, Seq[String]]] = request

    val formGroups = reviewPageData.formGroups(reportingPeriod, longForm)
    val action: Call = routes.MultiPageFormReviewController.postReview(request.companyDetail.companiesHouseId)
    val urlFunction: ReportId => String = (id: ReportId) => controllers.routes.ReportController.view(id).absoluteURL()

    emptyReview.bindForm.fold(
      errs => Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(errs, formGroups, action)))),
      review => {
        if (review.confirmed) verifyingOAuthScope(request.companyDetail.companiesHouseId, request.oAuthToken) {
          for {
            reportId <- createReport(request.companyDetail, request.emailAddress, reportingPeriod, longForm, review.confirmedBy, urlFunction)
            _ <- clearFormData
          } yield Redirect(controllers.routes.ConfirmationController.showConfirmation(reportId))
        } else {
          Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(emptyReview.fill(review), formGroups, action))))
        }
      }
    )
  }

  private def createReport(companyDetail: CompanyDetail, emailAddress: String, reportingPeriod: ReportingPeriodFormModel, longForm: LongFormModel, confirmedBy: String, urlFunction: ReportId => String): Future[ReportId] = {
    for {
      reportId <- reports.createLongReport(companyDetail, reportingPeriod, longForm, confirmedBy, emailAddress, urlFunction)
      _ <- Future.successful(confirmationActor ! 'poll)
    } yield reportId
  }
}
