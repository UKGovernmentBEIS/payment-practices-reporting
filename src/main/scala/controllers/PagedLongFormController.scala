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
import views.html.helpers.ReviewPageData

import scala.concurrent.ExecutionContext

/**
  * @tparam T the type of the form that is being processed by this page
  * @tparam U the type of the form that will be processed by the next page
  */
case class FormHandler[T, U](
  form: Form[T],
  nextForm: Form[U],
  private val errorFunction: (Html, CompanyDetail) => (Form[T], Map[String, String]) => Html,
  private val nextPageFunction: (Html, CompanyDetail) => (Form[U], Map[String, String]) => Html
) {
  def bind(implicit request: Request[Map[String, Seq[String]]]): FormHandler[T, U] = copy(form = form.bindForm)

  def errorPage(reportPageHeader: Html, companyDetail: CompanyDetail, otherFormData: Map[String, String]): Html =
    errorFunction(reportPageHeader, companyDetail)(form, otherFormData)

  def nextPage(reportPageHeader: Html, companyDetail: CompanyDetail, dataForPage: Map[String, String], otherFormData: Map[String, String]): Html =
    nextPageFunction(reportPageHeader, companyDetail)(nextForm.bind(dataForPage).discardingErrors, otherFormData ++ form.data)
}

class PagedLongFormController @Inject()(
  reports: ReportService,
  validations: Validations,
  pageFormData: PagedLongFormData,
  val companyAuth: CompanyAuthService,
  companyAuthAction: CompanyAuthAction,
  val serviceConfig: ServiceConfig,
  val pageConfig: PageConfig,
  @Named("confirmation-actor") confirmationActor: ActorRef
)(implicit val ec: ExecutionContext, messages: MessagesApi) extends Controller with BaseFormController with PageHelper {

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
      (header: Html, companyDetail: CompanyDetail) => (errs: Form[ReportingPeriodFormModel], data) => pages.reportingPeriod(header, errs, data, companyDetail.companiesHouseId, df, serviceStartDate),
      (header: Html, companyDetail: CompanyDetail) => (form: Form[PaymentStatisticsForm], data) => pages.longFormPage1(header, form, data, companyDetail.companiesHouseId, df, serviceStartDate)
    ),
    FormHandler(
      emptyPaymentStatisticsForm,
      emptyPaymentTermsForm,
      (header: Html, companyDetail: CompanyDetail) => (errs: Form[PaymentStatisticsForm], data) => pages.longFormPage1(header, errs, data, companyDetail.companiesHouseId, df, serviceStartDate),
      (header: Html, companyDetail: CompanyDetail) => (form: Form[PaymentTermsForm], data) => pages.longFormPage2(header, form, data, companyDetail.companiesHouseId, df, serviceStartDate)
    ),
    FormHandler(
      emptyPaymentTermsForm,
      emptyDisputeResolutionForm,
      (header: Html, companyDetail: CompanyDetail) => (errs: Form[PaymentTermsForm], data) => pages.longFormPage2(header, errs, data, companyDetail.companiesHouseId, df, serviceStartDate),
      (header: Html, companyDetail: CompanyDetail) => (form: Form[DisputeResolutionForm], data) => pages.longFormPage3(header, form, data, companyDetail.companiesHouseId, df, serviceStartDate)
    ),
    FormHandler(
      emptyDisputeResolutionForm,
      emptyOtherInformationForm,
      (header: Html, companyDetail: CompanyDetail) => (errs: Form[DisputeResolutionForm], data) => pages.longFormPage3(header, errs, data, companyDetail.companiesHouseId, df, serviceStartDate),
      (header: Html, companyDetail: CompanyDetail) => (form: Form[OtherInformationForm], data) => pages.longFormPage4(header, form, data, companyDetail.companiesHouseId, df, serviceStartDate)
    ),
    FormHandler(
      emptyOtherInformationForm,
      emptyLongForm,
      (header: Html, companyDetail: CompanyDetail) => (errs: Form[OtherInformationForm], data) => pages.longFormPage4(header, errs, data, companyDetail.companiesHouseId, df, serviceStartDate),
      (header: Html, companyDetail: CompanyDetail) => (form: Form[LongFormModel], data) => {
        val boundData = for {
          lf <- emptyLongForm.bind(data).value
          reportingPeriod <- emptyReportingPeriod.bind(data).value
        } yield (lf, reportingPeriod)

        boundData match {
          case Some((lf, reportingPeriod)) =>
            val formGroups = ReviewPageData.formGroups(companyDetail.companyName, reportingPeriod, lf)
            val action = routes.LongFormController.postReview(companyDetail.companiesHouseId)
            pages.review(emptyReview, data, formGroups, action)

          // There were errors on the LongForm. Because the LongForm binds the same structures as the
          // individual page forms, we should never get here, but we need a sensible response if we do
          case None => ???
        }
      }
    )
  )

  def postFormPage(pageNumber: Int, companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId)(parse.urlFormEncoded) { implicit request =>
    if (pageNumber < 1 || pageNumber >= emptyFormHandlers.length) NotFound
    else handlePage(pageNumber, request.companyDetail)
  }

  private def handlePage(pageNumber: Int, companyDetail: CompanyDetail)(implicit request: CompanyAuthRequest[Map[String, Seq[String]]]): Result = {
    val title = publishTitle(request.companyDetail.companyName)

    val boundForms = emptyFormHandlers.map(_.bind)
    val handlersUpToThisPage = boundForms.take(pageNumber + 1)

    val errorResult = handlersUpToThisPage.foldLeft(None: Option[Result]) { (currentResult, formHandler) =>
      currentResult match {
        case Some(r) => Some(r)
        case None    =>
          if (formHandler.form.hasErrors) {
            val formsToStash = boundForms.take(pageNumber) ++ boundForms.drop(pageNumber + 1)
            val dataToStash = formsToStash.foldLeft(Map[String, String]())((acc, handler) => acc ++ handler.form.data)
            Some(BadRequest(page(title)(formHandler.errorPage(reportPageHeader, companyDetail, dataToStash))))
          }
          else None
      }
    }

    errorResult match {
      case Some(r) => r
      case None    =>
        boundForms.drop(pageNumber).headOption.map { handlerForThisPage =>
          val formsToStash = boundForms.take(pageNumber) ++ boundForms.drop(pageNumber + 2)
          val dataForNextPage = boundForms.drop(pageNumber + 1).headOption.map(_.form.data).getOrElse(Map.empty[String, String])
          val dataToStash = formsToStash.foldLeft(Map[String, String]())((acc, handler) => acc ++ handler.form.data)
          Ok(page(reviewPageTitle)(home, handlerForThisPage.nextPage(reportPageHeader, companyDetail, dataForNextPage, dataToStash)))
        }.getOrElse(NotFound)
    }
  }
}
