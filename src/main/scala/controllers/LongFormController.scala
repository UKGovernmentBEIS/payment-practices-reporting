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
import play.api.data.Form
import play.api.data.Forms.{single, text}
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import play.api.{Logger, UnexpectedException}
import play.twirl.api.Html
import services._
import views.html.helpers.ReviewPageData

import scala.concurrent.{ExecutionContext, Future}
import scala.language.existentials
import scala.util.Random

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
  extends Controller with BaseFormController with PageHelper with FormSessionHelpers {

  import longFormPageModel._
  import validations._
  import views.html.{report => pages}

  private val reviewPageTitle = "Review your report"

  private def publishTitle(companyName: String) = s"Publish a report for $companyName"

  def reportPageHeader(implicit request: CompanyAuthRequest[_]): Html = h1(s"Publish a report for:<br>${request.companyDetail.companyName}")

  def show(formName: LongFormName, companiesHouseId: CompaniesHouseId): Action[AnyContent] = companyAuthAction(companiesHouseId).async { implicit request =>
    val title = publishTitle(request.companyDetail.companyName)

    val handlerForThisPage = handlerFor(formName)

    bindUpToPage(formHandlers, formName).map {
      case FormIsOk(handler)      => Ok(page(title)(handler.renderPage(reportPageHeader, request.companyDetail)))
      case FormHasErrors(handler) =>
        if (handler.formName == handlerForThisPage.formName)
          BadRequest(page(title)(handler.renderPage(reportPageHeader, request.companyDetail)))
        else
          Redirect(handler.pageCall(request.companyDetail))
      case FormIsBlank(handler)   =>
        if (handler.formName == handlerForThisPage.formName)
          Ok(page(title)(handlerForThisPage.renderPage(reportPageHeader, request.companyDetail)))
        else
          Redirect(handler.pageCall(request.companyDetail))
    }
  }

  def post(formName: LongFormName, companiesHouseId: CompaniesHouseId): Action[Map[String, Seq[String]]] =
    companyAuthAction(companiesHouseId).async(parse.urlFormEncoded) {
      implicit request =>
        val handler = handlerFor(formName)

        for {
          _ <- saveFormData(handler.formName, handler.bind.form)
          result <- handlePostFormPage(formName, request.companyDetail)
        } yield result
    }

  private def handlePostFormPage(formName: LongFormName, companyDetail: CompanyDetail)(implicit request: CompanyAuthRequest[Map[String, Seq[String]]]): Future[Result] = {
    val title = publishTitle(companyDetail.companyName)

    bindUpToPage(formHandlers, formName).map {
      case FormHasErrors(handler) => BadRequest(page(title)(handler.renderPage(reportPageHeader, companyDetail)))
      case FormIsOk(handler)      => Redirect(handler.nextPageCall(companyDetail))
      case FormIsBlank(handler)   => Ok(page(title)(handler.renderPage(reportPageHeader, request.companyDetail)))
    }
  }

  private def bindLongForm(implicit sessionId: SessionId): Future[Option[LongFormModel]] = {
    sessionService.get[JsObject](sessionId, formDataSessionKey).map {
      case None       => None
      case Some(data) =>
        for {
          ps <- emptyPaymentStatisticsForm.bind((data \\ PaymentStatistics.entryName).headOption.getOrElse(Json.obj())).value
          pt <- emptyPaymentTermsForm.bind((data \\ PaymentTerms.entryName).headOption.getOrElse(Json.obj())).value
          dr <- emptyDisputeResolutionForm.bind((data \\ DisputeResolution.entryName).headOption.getOrElse(Json.obj())).value
          oi <- emptyOtherInformationForm.bind((data \\ OtherInformation.entryName).headOption.getOrElse(Json.obj())).value
        } yield LongFormModel(ps.paymentStatistics, pt.paymentTerms, dr.disputeResolution, oi.otherInformation)
    }
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

  private def handleBinding[T](request: CompanyAuthRequest[T], f: (CompanyAuthRequest[T], ReportingPeriodFormModel, LongFormModel) => Future[Result]) = {
    implicit val req: CompanyAuthRequest[T] = request

    bindAllPages(formHandlers).flatMap {
      case FormHasErrors(handler) => Future.successful(Redirect(handler.pageCall(request.companyDetail)))
      case FormIsBlank(handler)   => Future.successful(Redirect(handler.pageCall(request.companyDetail)))
      case FormIsOk(handler)      =>
        val forms = for {
          reportingPeriod <- loadFormData(emptyReportingPeriod, LongFormName.ReportingPeriod).map(_.value)
          longForm <- bindLongForm
        } yield (reportingPeriod, longForm)

        forms.flatMap {
          case (Some(r), Some(lf)) => f(request, r, lf)

          // The following cases should not happen - if one of them does it indicates
          // some kind of mismatch between the FormHandlers and the base form models
          case (_, _) =>
            val ref = Random.nextInt(1000000)
            Logger.error(s"Error reference $ref: The reporting period and/or long forms did not bind correctly")
            throw UnexpectedException(Some(s"Error reference $ref"))
        }
    }
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

    emptyReview.bindForm.fold(
      errs => Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(errs, formGroups, action)))),
      review => {
        if (review.confirmed) verifyingOAuthScope(request.companyDetail.companiesHouseId, request.oAuthToken) {
          createReport(request.companyDetail.companiesHouseId, reportingPeriod, longForm, review.confirmedBy).map(rId => Redirect(controllers.routes.ConfirmationController.showConfirmation(rId)))
        } else {
          Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(emptyReview.fill(review), formGroups, action))))
        }
      }
    )
  }

  private def createReport(companiesHouseId: CompaniesHouseId, reportingPeriod: ReportingPeriodFormModel, longForm: LongFormModel, confirmedBy: String)(implicit request: CompanyAuthRequest[_]): Future[ReportId] = {
    val urlFunction: ReportId => String = (id: ReportId) => controllers.routes.ReportController.view(id).absoluteURL()
    for {
      reportId <- reports.createLongReport(request.companyDetail, reportingPeriod, longForm, confirmedBy, request.emailAddress, urlFunction)
      _ <- Future.successful(confirmationActor ! 'poll)
    } yield reportId
  }
}
