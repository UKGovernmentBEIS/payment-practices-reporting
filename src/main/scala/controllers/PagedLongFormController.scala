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
import models.CompaniesHouseId
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Controller, Result}
import play.twirl.api.Html
import services._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.existentials

class PagedLongFormController @Inject()(
  reports: ReportService,
  validations: Validations,
  pageFormData: PagedLongFormData,
  val companyAuth: CompanyAuthService,
  companyAuthAction: CompanyAuthAction,
  val serviceConfig: ServiceConfig,
  val pageConfig: PageConfig,
  val sessionService: SessionService,
  reportingPeriodController: ReportingPeriodController,
  @Named("confirmation-actor") confirmationActor: ActorRef
)(implicit val ec: ExecutionContext, messages: MessagesApi) extends Controller with BaseFormController with PageHelper with FormSessionHelpers {

  import pageFormData._
  import validations._
  import views.html.{report => pages}

  private val reviewPageTitle = "Review your report"

  private def publishTitle(companyName: String) = s"Publish a report for $companyName"

  def reportPageHeader(implicit request: CompanyAuthRequest[_]): Html = h1(s"Publish a report for:<br>${request.companyDetail.companyName}")

  val emptyFormHandlers = Seq(
    FormHandler(
      reportingPeriodController.reportPeriodDataSessionKey,
      emptyReportingPeriod,
      (header: Html, companyDetail: CompanyDetail) => (form: Form[ReportingPeriodFormModel]) => pages.reportingPeriod(header, form, companyDetail.companiesHouseId, df, serviceStartDate),
      (companyDetail: CompanyDetail) => routes.ReportingPeriodController.show(companyDetail.companiesHouseId),
      (companyDetail: CompanyDetail) => routes.PagedLongFormController.show(1, companyDetail.companiesHouseId)
    ),
    FormHandler(
      "page1",
      emptyPaymentStatisticsForm,
      (header: Html, companyDetail: CompanyDetail) => (form: Form[PaymentStatisticsForm]) => pages.longFormPage1(header, form, companyDetail.companiesHouseId, df, serviceStartDate),
      (companyDetail: CompanyDetail) => routes.PagedLongFormController.show(1, companyDetail.companiesHouseId),
      (companyDetail: CompanyDetail) => routes.PagedLongFormController.show(2, companyDetail.companiesHouseId)
    ),
    FormHandler(
      "page2",
      emptyPaymentTermsForm,
      (header: Html, companyDetail: CompanyDetail) => (form: Form[PaymentTermsForm]) => pages.longFormPage2(header, form, companyDetail.companiesHouseId, df, serviceStartDate),
      (companyDetail: CompanyDetail) => routes.PagedLongFormController.show(2, companyDetail.companiesHouseId),
      (companyDetail: CompanyDetail) => routes.PagedLongFormController.show(3, companyDetail.companiesHouseId)
    ),
    FormHandler(
      "page3",
      emptyDisputeResolutionForm,
      (header: Html, companyDetail: CompanyDetail) => (form: Form[DisputeResolutionForm]) => pages.longFormPage3(header, form, companyDetail.companiesHouseId, df, serviceStartDate),
      (companyDetail: CompanyDetail) => routes.PagedLongFormController.show(3, companyDetail.companiesHouseId),
      (companyDetail: CompanyDetail) => routes.PagedLongFormController.show(4, companyDetail.companiesHouseId)
    ),
    FormHandler(
      "page4",
      emptyOtherInformationForm,
      (header: Html, companyDetail: CompanyDetail) => (form: Form[OtherInformationForm]) => pages.longFormPage4(header, form, companyDetail.companiesHouseId, df, serviceStartDate),
      (companyDetail: CompanyDetail) => routes.PagedLongFormController.show(4, companyDetail.companiesHouseId),
      (companyDetail: CompanyDetail) => routes.PagedLongFormController.showReview(companyDetail.companiesHouseId)
    )
  )

  case class LongFormPageModel(
    page1: Option[PaymentStatistics],
    page2: Option[PaymentTerms],
    page3: Option[DisputeResolution],
    page4: Option[OtherInformation]
  )

  sealed trait FormResult
  case object Start extends FormResult
  case class FormIsBlank(formHandler: FormHandler[_]) extends FormResult
  case class FormHasErrors(formHandler: FormHandler[_]) extends FormResult
  case class FormIsOk(formHandler: FormHandler[_]) extends FormResult

  def show(pageNumber: Int, companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId).async { implicit request =>
    val title = publishTitle(request.companyDetail.companyName)

    emptyFormHandlers.drop(pageNumber).headOption.map { handlerForThisPage =>
      bindUpToPage(pageNumber).map {
        case Start                  => NotFound // something went wrong - no handlers in the list!?
        case FormIsOk(handler)      => Ok(page(title)(handler.renderPage(reportPageHeader, request.companyDetail)))
        case FormHasErrors(handler) =>
          if (handler.sessionKey == handlerForThisPage.sessionKey)
            BadRequest(page(title)(handler.renderPage(reportPageHeader, request.companyDetail)))
          else
            Redirect(handler.pageCall(request.companyDetail))
        case FormIsBlank(handler)   =>
          if (handler.sessionKey == handlerForThisPage.sessionKey)
            Ok(page(title)(handler.renderPage(reportPageHeader, request.companyDetail)))
          else
            Redirect(handler.pageCall(request.companyDetail))
      }
    }.getOrElse(Future.successful(NotFound))
  }

  private def bindUpToPage(pageNumber: Int)(implicit request: CompanyAuthRequest[_]) = {
    emptyFormHandlers.take(pageNumber + 1).foldLeft(Future.successful(Start): Future[FormResult]) { (currentResult, nextHandler) =>
      currentResult.flatMap {
        case Start | FormIsOk(_) =>
          bindFormDataFromSession(nextHandler).map { boundHandler =>
            if (boundHandler.form.data.isEmpty && boundHandler.form.value.isEmpty) FormIsBlank(boundHandler)
            else if (boundHandler.form.hasErrors) FormHasErrors(boundHandler)
            else FormIsOk(boundHandler)
          }
        case notOk               => Future.successful(notOk)
      }
    }
  }

  def post(pageNumber: Int, companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId).async(parse.urlFormEncoded) { implicit request =>
    val handlerForThisPage = emptyFormHandlers.drop(pageNumber).headOption

    handlerForThisPage match {
      case None          => Future.successful(NotFound)
      case Some(handler) => for {
        _ <- saveFormData(handler.sessionKey, handler.bind.form)
        result <- handlePostFormPage(pageNumber, request.companyDetail)
      } yield result
    }
  }

  private def handlePostFormPage(pageNumber: Int, companyDetail: CompanyDetail)(implicit request: CompanyAuthRequest[Map[String, Seq[String]]]): Future[Result] = {
    val title = publishTitle(companyDetail.companyName)

    bindUpToPage(pageNumber).map {
      case Start                  => NotFound // something went wrong - no handlers in the list!?
      case FormHasErrors(handler) => BadRequest(page(title)(handler.renderPage(reportPageHeader, companyDetail)))
      case FormIsOk(handler)      => Redirect(handler.nextPageCall(companyDetail))
      case FormIsBlank(handler)   => Ok(page(title)(handler.renderPage(reportPageHeader, request.companyDetail)))
    }
  }

  def showReview(companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId).async { implicit request =>

    ???
  }

}
