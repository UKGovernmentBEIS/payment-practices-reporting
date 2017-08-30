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
import controllers.FormPageModels.LongFormName._
import controllers.FormPageModels._
import forms.report.{LongFormModel, ReportingPeriodFormModel, Validations}
import models.{CompaniesHouseId, ReportId}
import org.scalactic.TripleEquals._
import play.api.data.Form
import play.api.data.Forms.{single, text}
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc._
import play.twirl.api.Html
import services._
import views.html.helpers.ReviewPageData

import scala.concurrent.{ExecutionContext, Future}
import scala.language.existentials

class LongFormController @Inject()(
  reports: ReportService,
  validations: Validations,
  val companyAuth: CompanyAuthService,
  companyAuthAction: CompanyAuthAction,
  val serviceConfig: ServiceConfig,
  val pageConfig: PageConfig,
  val sessionService: SessionService,
  longFormPageModel: LongFormPageModel,
  @Named("confirmation-actor") confirmationActor: ActorRef
)(implicit val ec: ExecutionContext, messages: MessagesApi)
  extends Controller
    with BaseFormController
    with PageHelper
    with FormSessionHelpers
    with FormControllerHelpers[LongFormModel, LongFormName] {

  import longFormPageModel._
  import validations._
  import views.html.{report => pages}

  private val reviewPageTitle = "Review your report"

  private def publishTitle(companyName: String) = s"Publish a report for $companyName"

  def reportPageHeader(companyDetail: CompanyDetail): Html = h1(s"Publish a report for:<br>${companyDetail.companyName}")

  override def formHandlers: Seq[LongFormHandler[_]] = longFormPageModel.formHandlers

  override val emptyReportingPeriod: Form[ReportingPeriodFormModel] = validations.emptyReportingPeriod

  override def bindReportingPeriod(implicit sessionId: SessionId): Future[Option[ReportingPeriodFormModel]] =
    loadFormData(emptyReportingPeriod, LongFormName.ReportingPeriod).map(_.value)

  def show(formName: LongFormName, companiesHouseId: CompaniesHouseId): Action[AnyContent] = companyAuthAction(companiesHouseId).async { implicit request =>
    val companyDetail = request.companyDetail

    handleShowPage(formName, companyDetail)
  }

  private[controllers] def handleShowPage(formName: LongFormName, companyDetail: CompanyDetail)(implicit sessionId: SessionId, pageContext: PageContext) = {
    val title = publishTitle(companyDetail.companyName)

    bindUpToPage(formHandlers, formName).map {
      case FormHasErrors(boundHandler) if boundHandler.formName !== formName => Redirect(boundHandler.callPage(companyDetail))
      case FormIsBlank(boundHandler) if boundHandler.formName !== formName   => Redirect(boundHandler.callPage(companyDetail))

      case FormHasErrors(boundHandler) => BadRequest(page(title)(boundHandler.renderPage(reportPageHeader(companyDetail), companyDetail)))
      // Form is blank, so the user hasn't filled it in yet. In this case we don't
      // want to show errors, so use the empty form handler for the formName
      case FormIsBlank(_) => Ok(page(title)(handlerFor(formName).renderPage(reportPageHeader(companyDetail), companyDetail)))

      case FormIsOk(handler, value) => Ok(page(title)(handler.renderPage(reportPageHeader(companyDetail), companyDetail)))
    }
  }

  //noinspection TypeAnnotation
  def post(formName: LongFormName, companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId).async(parse.urlFormEncoded) { implicit request =>
    val handler = handlerFor(formName)

    for {
      _ <- saveFormData(handler.formName, handler.bind.form)
      result <- handlePostFormPage(formName, request.companyDetail)
    } yield result
  }

  private def handlePostFormPage(formName: LongFormName, companyDetail: CompanyDetail)(implicit sessionId: SessionId, pageContext: PageContext): Future[Result] = {
    val title = publishTitle(companyDetail.companyName)

    bindUpToPage(formHandlers, formName).map {
      case FormHasErrors(handler) => BadRequest(page(title)(handler.renderPage(reportPageHeader(companyDetail), companyDetail)))
      case FormIsBlank(handler)   => BadRequest(page(title)(handler.renderPage(reportPageHeader(companyDetail), companyDetail)))

      case FormIsOk(handler, value) => nextFormHandler(handler) match {
        case Some(nextHandler) => Redirect(nextHandler.callPage(companyDetail))
        case None              => Redirect(routes.LongFormController.showReview(companyDetail.companiesHouseId))
      }
    }
  }

  def bindMainForm(implicit sessionId: SessionId): Future[Option[LongFormModel]] =
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

    if (revise) Future.successful(Redirect(routes.ReportingPeriodController.show(companiesHouseId)))
    else handleBinding(request, handleReviewPost)
  }

  private def renderReview(request: CompanyAuthRequest[_], r: ReportingPeriodFormModel, lf: LongFormModel): Future[Result] = {
    implicit val req: CompanyAuthRequest[_] = request

    val title = publishTitle(request.companyDetail.companyName)
    val action: Call = routes.LongFormController.postReview(request.companyDetail.companiesHouseId)
    val formGroups = ReviewPageData.formGroups(request.companyDetail.companyName, r, lf)
    Future.successful(Ok(page(title)(views.html.report.review(emptyReview, formGroups, action))))
  }

  private def handleReviewPost(request: CompanyAuthRequest[Map[String, Seq[String]]], reportingPeriod: ReportingPeriodFormModel, longForm: LongFormModel): Future[Result] = {
    implicit val req: CompanyAuthRequest[Map[String, Seq[String]]] = request

    val formGroups = ReviewPageData.formGroups(request.companyDetail.companyName, reportingPeriod, longForm)
    val action: Call = routes.LongFormController.postReview(request.companyDetail.companiesHouseId)
    val urlFunction: ReportId => String = (id: ReportId) => controllers.routes.ReportController.view(id).absoluteURL()

    emptyReview.bindForm.fold(
      errs => Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(errs, formGroups, action)))),
      review => {
        if (review.confirmed) verifyingOAuthScope(request.companyDetail.companiesHouseId, request.oAuthToken) {
          createReport(request.companyDetail, request.emailAddress, reportingPeriod, longForm, review.confirmedBy, urlFunction)
            .map(reportId => Redirect(controllers.routes.ConfirmationController.showConfirmation(reportId)))
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
