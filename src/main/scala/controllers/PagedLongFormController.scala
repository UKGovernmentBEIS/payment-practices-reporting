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
import play.api.mvc.{Controller, Request, Result}
import play.twirl.api.Html
import services._

import scala.concurrent.{ExecutionContext, Future}

/**
  * @tparam T the type of the form that is being processed by this page
  * @tparam U the type of the form that will be processed by the next page
  */
case class FormHandler[T, U](
  form: Form[T],
  nextForm: Form[U],
  private val formPageFunction: (Html, CompanyDetail) => (Form[T]) => Html,
  private val nextPageFunction: (Html, CompanyDetail) => (Form[U]) => Html
) {
  def bind(implicit request: Request[Map[String, Seq[String]]]): FormHandler[T, U] = copy(form = form.bindForm)

  def bind(data: Map[String, String]): FormHandler[T, U] = copy(form = form.bind(data))

  def formPage(reportPageHeader: Html, companyDetail: CompanyDetail): Html =
    formPageFunction(reportPageHeader, companyDetail)(form)

  def nextPage(reportPageHeader: Html, companyDetail: CompanyDetail, dataForPage: Map[String, String]): Html =
    nextPageFunction(reportPageHeader, companyDetail)(nextForm.bind(dataForPage).discardingErrors)
}

class PagedLongFormController @Inject()(
  reports: ReportService,
  validations: Validations,
  pageFormData: PagedLongFormData,
  val companyAuth: CompanyAuthService,
  companyAuthAction: CompanyAuthAction,
  val serviceConfig: ServiceConfig,
  val pageConfig: PageConfig,
  val sessionService: SessionService,
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
      emptyReportingPeriod,
      emptyPaymentStatisticsForm,
      (header: Html, companyDetail: CompanyDetail) => (errs: Form[ReportingPeriodFormModel]) => pages.reportingPeriod(header, errs, companyDetail.companiesHouseId, df, serviceStartDate),
      (header: Html, companyDetail: CompanyDetail) => (form: Form[PaymentStatisticsForm]) => pages.longFormPage1(header, form, companyDetail.companiesHouseId, df, serviceStartDate)
    ),
    FormHandler(
      emptyPaymentStatisticsForm,
      emptyPaymentTermsForm,
      (header: Html, companyDetail: CompanyDetail) => (errs: Form[PaymentStatisticsForm]) => pages.longFormPage1(header, errs, companyDetail.companiesHouseId, df, serviceStartDate),
      (header: Html, companyDetail: CompanyDetail) => (form: Form[PaymentTermsForm]) => pages.longFormPage2(header, form, companyDetail.companiesHouseId, df, serviceStartDate)
    ),
    FormHandler(
      emptyPaymentTermsForm,
      emptyDisputeResolutionForm,
      (header: Html, companyDetail: CompanyDetail) => (errs: Form[PaymentTermsForm]) => pages.longFormPage2(header, errs, companyDetail.companiesHouseId, df, serviceStartDate),
      (header: Html, companyDetail: CompanyDetail) => (form: Form[DisputeResolutionForm]) => pages.longFormPage3(header, form, companyDetail.companiesHouseId, df, serviceStartDate)
    ),
    FormHandler(
      emptyDisputeResolutionForm,
      emptyOtherInformationForm,
      (header: Html, companyDetail: CompanyDetail) => (errs: Form[DisputeResolutionForm]) => pages.longFormPage3(header, errs, companyDetail.companiesHouseId, df, serviceStartDate),
      (header: Html, companyDetail: CompanyDetail) => (form: Form[OtherInformationForm]) => pages.longFormPage4(header, form, companyDetail.companiesHouseId, df, serviceStartDate)
    ),
    FormHandler(
      emptyOtherInformationForm,
      emptyLongForm,
      (header: Html, companyDetail: CompanyDetail) => (errs: Form[OtherInformationForm]) => pages.longFormPage4(header, errs, companyDetail.companiesHouseId, df, serviceStartDate),
      (header: Html, companyDetail: CompanyDetail) => (form: Form[LongFormModel]) => ???
    )
  )

  def bindFormDataFromSession(formHandler: FormHandler[_, _], pageNumber: Int)(implicit request: CompanyAuthRequest[_]): Future[FormHandler[_, _]] = {
    sessionService.get[Map[String, String]](request.sessionId, s"page$pageNumber").map {
      case None           => formHandler
      case Some(formData) => formHandler.bind(formData)
    }
  }

  case class LongFormPageModel(
    page1: Option[PaymentStatistics],
    page2: Option[PaymentTerms],
    page3: Option[DisputeResolution],
    page4: Option[OtherInformation]
  )

  def show(pageNumber: Int, companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId).async { implicit request =>
    val title = publishTitle(request.companyDetail.companyName)

    emptyFormHandlers.drop(pageNumber).headOption.map {
      formHandler =>
        bindFormDataFromSession(formHandler, pageNumber).map { boundFormHandler =>
          if (boundFormHandler.form.hasErrors) BadRequest(page(title)(boundFormHandler.formPage(reportPageHeader, request.companyDetail)))
          else Ok(page(title)(boundFormHandler.formPage(reportPageHeader, request.companyDetail)))
        }
    }.getOrElse(Future.successful(NotFound))
  }

  def post(pageNumber: Int, companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId)(parse.urlFormEncoded) { implicit request =>
    handlePostFormPage(pageNumber, request.companyDetail)
  }

  private def handlePostFormPage(pageNumber: Int, companyDetail: CompanyDetail)(implicit request: CompanyAuthRequest[Map[String, Seq[String]]]): Result = {
    val boundForms = emptyFormHandlers.map(_.bind)

    boundForms.drop(pageNumber).headOption.map { handlerForThisPage =>
      val title = publishTitle(request.companyDetail.companyName)
      val handlersUpToThisPage = boundForms.take(pageNumber + 1)

      val errorResult = handlersUpToThisPage.foldLeft(None: Option[Result]) { (currentResult, formHandler) =>
        currentResult match {
          case Some(r) => Some(r)
          case None    =>
            if (formHandler.form.hasErrors) Some(BadRequest(page(title)(formHandler.formPage(reportPageHeader, companyDetail))))
            else None
        }
      }

      errorResult match {
        case Some(r) => r
        case None    =>
          saveFormData(s"page$pageNumber", handlerForThisPage.form)
          val dataForNextPage = boundForms.drop(pageNumber + 1).headOption.map(_.form.data).getOrElse(Map.empty[String, String])
          Ok(page(reviewPageTitle)(home, handlerForThisPage.nextPage(reportPageHeader, companyDetail, dataForNextPage)))
      }
    }.getOrElse(NotFound)
  }
}
