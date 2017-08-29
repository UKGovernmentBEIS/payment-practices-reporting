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
import controllers.FormPageModels._
import forms.report._
import models.{CompaniesHouseId, ReportId}
import play.api.data.Form
import play.api.data.Forms.{single, _}
import play.api.i18n.MessagesApi
import play.api.mvc.{Call, Controller, Result}
import play.twirl.api.Html
import services._
import views.html.helpers.ReviewPageData

import scala.concurrent.{ExecutionContext, Future}

class ShortFormController @Inject()(
  reports: ReportService,
  validations: Validations,
  val companyAuth: CompanyAuthService,
  companyAuthAction: CompanyAuthAction,
  val serviceConfig: ServiceConfig,
  val pageConfig: PageConfig,
  val sessionService: SessionService,
  shortFormPageModel: ShortFormPageModel,
  @Named("confirmation-actor") confirmationActor: ActorRef
)(implicit val ec: ExecutionContext, messages: MessagesApi)
  extends Controller
    with BaseFormController
    with PageHelper
    with FormSessionHelpers
    with FormControllerHelpers[ShortFormModel, ShortFormName] {

  import shortFormPageModel._
  import validations._
  import views.html.{report => pages}

  private val reviewPageTitle = "Review your report"

  private def publishTitle(companyName: String) = s"Publish a report for $companyName"

  def reportPageHeader(implicit request: CompanyAuthRequest[_]): Html = h1(s"Publish a report for:<br>${request.companyDetail.companyName}")

  override def formHandlers: Seq[ShortFormHandler[_]] =
    shortFormPageModel.formHandlers

  override def bindMainForm(implicit sessionId: SessionId): Future[Option[ShortFormModel]] =
    loadFormData(emptyShortForm, ShortFormName.ShortForm).map(_.value)

  override def bindReportingPeriod(implicit sessionId: SessionId): Future[Option[ReportingPeriodFormModel]] =
    loadFormData(emptyReportingPeriod, ShortFormName.ReportingPeriod).map(_.value)

  override def emptyReportingPeriod: Form[ReportingPeriodFormModel] =
    validations.emptyReportingPeriod

  //noinspection TypeAnnotation
  def show(companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId).async { implicit request =>
    val title = publishTitle(request.companyDetail.companyName)

    checkValidFromSession(emptyReportingPeriod, ShortFormName.ReportingPeriod.entryName).flatMap {
      case false => Future.successful(Redirect(routes.ReportingPeriodController.show(companiesHouseId)))
      case true  => loadFormData(emptyShortForm, ShortFormName.ShortForm).map { form =>
        Ok(page(title)(home, pages.shortForm(reportPageHeader, form, companiesHouseId, df, serviceStartDate)))
      }
    }
  }

  //noinspection TypeAnnotation
  def post(companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId).async(parse.urlFormEncoded) { implicit request =>
    val handler = handlerFor(ShortFormName.ShortForm)

    for {
      _ <- saveFormData(handler.formName, handler.bind.form)
      result <- handlePostFormPage(handler.formName, request.companyDetail)
    } yield result
  }

  private def handlePostFormPage(formName: ShortFormName, companyDetail: CompanyDetail)(implicit request: CompanyAuthRequest[Map[String, Seq[String]]]): Future[Result] = {
    val title = publishTitle(companyDetail.companyName)

    bindUpToPage(formHandlers, formName).map {
      case FormHasErrors(handler) => BadRequest(page(title)(handler.renderPage(reportPageHeader, companyDetail)))
      case FormIsOk(handler)      => nextFormHandler(handler.formName) match {
        case Some(nextHandler) => Redirect(nextHandler.pageCall(companyDetail))
        case None              => Redirect(routes.ShortFormController.showReview(companyDetail.companiesHouseId))
      }
      case FormIsBlank(handler)   => Ok(page(title)(handler.renderPage(reportPageHeader, request.companyDetail)))
    }
  }

  //noinspection TypeAnnotation
  def showReview(companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId).async { implicit request =>
    handleBinding(request, renderReview)
  }

  //noinspection TypeAnnotation
  def postReview(companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId).async(parse.urlFormEncoded) { implicit request =>
    val f: (CompanyAuthRequest[Map[String, Seq[String]]], ReportingPeriodFormModel, ShortFormModel) => Future[Result] = handleReviewPost
    val revise: Boolean = Form(single("revise" -> text)).bindForm.value.contains("Revise")

    if (revise) Future.successful(Redirect(routes.ReportingPeriodController.show(companiesHouseId)))
    else handleBinding(request, f)
  }

  private def renderReview(request: CompanyAuthRequest[_], reportingPeriod: ReportingPeriodFormModel, shortForm: ShortFormModel): Future[Result] = {
    implicit val req: CompanyAuthRequest[_] = request

    val action: Call = routes.ShortFormController.postReview(request.companyDetail.companiesHouseId)
    val formGroups = ReviewPageData.formGroups(request.companyDetail.companyName, reportingPeriod, shortForm)
    Future.successful(Ok(page(reviewPageTitle)(home, pages.review(emptyReview, formGroups, action))))
  }

  private def handleReviewPost(request: CompanyAuthRequest[Map[String, Seq[String]]], r: ReportingPeriodFormModel, sf: ShortFormModel): Future[Result] = {
    implicit val req: CompanyAuthRequest[Map[String, Seq[String]]] = request

    val action: Call = routes.ShortFormController.postReview(request.companyDetail.companiesHouseId)
    val formGroups = ReviewPageData.formGroups(request.companyDetail.companyName, r, sf)
    emptyReview.bindForm.fold(
      errs => Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(errs, formGroups, action)))),
      review => {
        if (review.confirmed) verifyingOAuthScope(request.companyDetail.companiesHouseId, request.oAuthToken) {
          createReport(request.companyDetail.companiesHouseId, r, sf, review.confirmedBy).map(rId => Redirect(controllers.routes.ConfirmationController.showConfirmation(rId)))
        } else {
          Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(emptyReview.fill(review), formGroups, action))))
        }
      }
    )
  }

  private def checkConfirmation(companiesHouseId: CompaniesHouseId, reportingPeriod: ReportingPeriodFormModel, shortForm: ShortFormModel)
    (implicit request: CompanyAuthRequest[Map[String, Seq[String]]]): Future[Result] = {
    val action: Call = routes.ShortFormController.postReview(companiesHouseId)
    val companyName: String = request.companyDetail.companyName

    val reportData = emptyReportingPeriod.fill(reportingPeriod).data ++ emptyShortForm.fill(shortForm).data
    val formGroups = ReviewPageData.formGroups(companyName, reportingPeriod, shortForm)

    emptyReview.bindForm.fold(
      errs => Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(errs, formGroups, action)))),
      review => {
        if (review.confirmed) verifyingOAuthScope(companiesHouseId, request.oAuthToken) {
          createReport(companiesHouseId, reportingPeriod, shortForm, review.confirmedBy).map(rId => Redirect(controllers.routes.ConfirmationController.showConfirmation(rId)))
        } else {
          Future.successful(BadRequest(page(reviewPageTitle)(home, pages.review(emptyReview.fill(review), formGroups, action))))
        }
      }
    )
  }

  private def createReport(companiesHouseId: CompaniesHouseId, reportingPeriod: ReportingPeriodFormModel, shortForm: ShortFormModel, confirmedBy: String)(implicit request: CompanyAuthRequest[_]): Future[ReportId] = {
    val urlFunction: ReportId => String = (id: ReportId) => controllers.routes.ReportController.view(id).absoluteURL()
    for {
      reportId <- reports.createShortReport(request.companyDetail, reportingPeriod, shortForm, confirmedBy, request.emailAddress, urlFunction)
      _ <- Future.successful(confirmationActor ! 'poll)
    } yield reportId
  }
}
