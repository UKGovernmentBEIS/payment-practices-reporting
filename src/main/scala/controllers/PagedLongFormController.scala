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
import play.api.Logger
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Controller, Request, Result}
import play.twirl.api.Html
import services._

import scala.concurrent.ExecutionContext

case class FormHandler[T](
  form: Form[T],
  private val errorFunction: (Html, CompaniesHouseId) => (Form[T], Map[String, String]) => Html,
  private val nextPageFunction: (Html, CompaniesHouseId) => (Map[String, String]) => Html
) {
  def bind(implicit request: Request[Map[String, Seq[String]]]): FormHandler[T] = copy(form = form.bindForm)

  def errorPage(reportPageHeader: Html, companiesHouseId: CompaniesHouseId, otherFormData: Map[String, String]): Html =
    errorFunction(reportPageHeader, companiesHouseId)(form, otherFormData)

  def nextPage(reportPageHeader: Html, companiesHouseId: CompaniesHouseId, otherFormData: Map[String, String]): Html =
    nextPageFunction(reportPageHeader, companiesHouseId)(otherFormData ++ form.data)
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

  import validations._
  import pageFormData._
  import views.html.{report => pages}

  private val reviewPageTitle = "Review your report"

  private def publishTitle(companyName: String) = s"Publish a report for $companyName"

  def reportPageHeader(implicit request: CompanyAuthRequest[_]): Html = h1(s"Publish a report for:<br>${request.companyDetail.companyName}")

  val emptyFormHandlers = Seq(
    FormHandler(
      emptyReportingPeriod,
      (header: Html, companiesHouseId: CompaniesHouseId) => (errs: Form[ReportingPeriodFormModel], data) => pages.reportingPeriod(header, errs, data, companiesHouseId, df, serviceStartDate),
      (header: Html, companiesHouseId: CompaniesHouseId) => (data) => pages.longFormPage1(header, emptyPaymentStatisticsForm, data, companiesHouseId, df, serviceStartDate)
    ),
    FormHandler(
      emptyPaymentStatisticsForm,
      (header: Html, companiesHouseId: CompaniesHouseId) => (errs: Form[PaymentStatisticsForm], data) => pages.longFormPage1(header, errs, data, companiesHouseId, df, serviceStartDate),
      (header: Html, companiesHouseId: CompaniesHouseId) => (data) => pages.longFormPage2(header, emptyPaymentTermsForm, data, companiesHouseId, df, serviceStartDate)
    ),
    FormHandler(
      emptyPaymentTermsForm,
      (header: Html, companiesHouseId: CompaniesHouseId) => (errs: Form[PaymentTermsForm], data) => pages.longFormPage2(header, errs, data, companiesHouseId, df, serviceStartDate),
      (header: Html, companiesHouseId: CompaniesHouseId) => (data) => pages.longFormPage3(header, emptyDisputeResolutionForm, data, companiesHouseId, df, serviceStartDate)
    ),
    FormHandler(
      emptyDisputeResolutionForm,
      (header: Html, companiesHouseId: CompaniesHouseId) => (errs: Form[DisputeResolutionForm], data) => pages.longFormPage3(header, errs, data, companiesHouseId, df, serviceStartDate),
      (header: Html, companiesHouseId: CompaniesHouseId) => (data) => pages.longFormPage4(header, emptyOtherInformationForm, data, companiesHouseId, df, serviceStartDate)
    ),
    FormHandler(
      emptyOtherInformationForm,
      (header: Html, companiesHouseId: CompaniesHouseId) => (errs: Form[OtherInformationForm], data) => pages.longFormPage4(header, errs, data, companiesHouseId, df, serviceStartDate),
      (header: Html, companiesHouseId: CompaniesHouseId) => (data) => ???
    )
  )

  def postFormPage(pageNumber: Int, companiesHouseId: CompaniesHouseId) = companyAuthAction(companiesHouseId)(parse.urlFormEncoded) { implicit request =>
    handlePage(pageNumber, companiesHouseId)
  }

  private def handlePage(pageNumber: Int, companiesHouseId: CompaniesHouseId)(implicit request: CompanyAuthRequest[Map[String, Seq[String]]]) = {
    val title = publishTitle(request.companyDetail.companyName)
    Logger.debug(request.body.toString)

    val longForm = emptyLongForm.bindForm

    val boundForms = emptyFormHandlers.map(_.bind)

    val handlerForThisPage = boundForms.drop(pageNumber).head
    val handlersUpToThisPage = boundForms.take(pageNumber + 1)
    val formsToStash = boundForms.filterNot(_ == handlerForThisPage)
    Logger.debug(formsToStash.toString)
    val dataToStash = formsToStash.foldLeft(Map[String, String]())((acc, handler) => acc ++ handler.form.data)
    Logger.debug(dataToStash.toString)

    val result = handlersUpToThisPage.foldLeft(None: Option[Result]) { (currentResult, formHandler) =>
      currentResult match {
        case Some(r) => Some(r)
        case None    =>
          if (formHandler.form.hasErrors) Some(BadRequest(page(title)(formHandler.errorPage(reportPageHeader, companiesHouseId, dataToStash))))
          else None
      }
    }

    result match {
      case Some(r) => r
      case None    => Ok(page(reviewPageTitle)(home, handlersUpToThisPage.last.nextPage(reportPageHeader, companiesHouseId, dataToStash)))
    }
  }


}
