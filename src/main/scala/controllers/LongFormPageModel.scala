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

import javax.inject.Inject

import config.ServiceConfig
import controllers.FormPageModels.LongFormName._
import controllers.FormPageModels.{LongFormHandler, LongFormName}
import forms.report.{ReportingPeriodFormModel, Validations}
import org.joda.time.format.DateTimeFormat
import play.api.data.Forms.mapping
import play.api.data.{Form, Mapping}
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import services.CompanyDetail

case class PaymentStatisticsForm(paymentStatistics: forms.report.PaymentStatistics)
case class PaymentTermsForm(paymentTerms: forms.report.PaymentTerms)
case class DisputeResolutionForm(disputeResolution: forms.report.DisputeResolution)
case class OtherInformationForm(otherInformation: forms.report.OtherInformation)

class LongFormPageModel @Inject()(validations: Validations, serviceConfig: ServiceConfig)(implicit messagesApi: MessagesApi)
  extends FormPageModel[LongFormHandler[_], LongFormName] {

  import validations._
  import views.html.{report => pages}

  private val df = DateTimeFormat.forPattern("d MMMM YYYY")

  private val serviceStartDate = serviceConfig.startDate.getOrElse(ServiceConfig.defaultServiceStartDate)

  def formHandlers: Seq[LongFormHandler[_]] = LongFormName.values.map(handlerFor)

  val paymentStatisticsFormMapping: Mapping[PaymentStatisticsForm] = mapping(
    PaymentStatistics.entryName -> paymentStatistics
  )(PaymentStatisticsForm.apply)(PaymentStatisticsForm.unapply)

  val paymentTermsFormMapping: Mapping[PaymentTermsForm] = mapping(
    PaymentTerms.entryName -> paymentTerms
  )(PaymentTermsForm.apply)(PaymentTermsForm.unapply)

  val disputeResolutionFormMapping: Mapping[DisputeResolutionForm] = mapping(
    DisputeResolution.entryName -> disputeResolution
  )(DisputeResolutionForm.apply)(DisputeResolutionForm.unapply)

  val otherInformationFormMapping: Mapping[OtherInformationForm] = mapping(
    OtherInformation.entryName -> otherInformation
  )(OtherInformationForm.apply)(OtherInformationForm.unapply)

  val emptyPaymentStatisticsForm: Form[PaymentStatisticsForm] = Form(paymentStatisticsFormMapping)
  val emptyPaymentTermsForm     : Form[PaymentTermsForm]      = Form(paymentTermsFormMapping)
  val emptyDisputeResolutionForm: Form[DisputeResolutionForm] = Form(disputeResolutionFormMapping)
  val emptyOtherInformationForm : Form[OtherInformationForm]  = Form(otherInformationFormMapping)

  def nextFormName(formName: LongFormName): Option[LongFormName] =
    LongFormName.values.dropWhile(_ != formName).drop(1).headOption

  def nextFormHandler(formName: LongFormName): Option[LongFormHandler[_]] =
    nextFormName(formName).map(handlerFor)

  def handlerFor(formName: LongFormName): LongFormHandler[_] = formName match {
    case ReportingPeriod   =>
      FormHandler(
        ReportingPeriod,
        emptyReportingPeriod,
        (header: Html, companyDetail: CompanyDetail) => (form: Form[ReportingPeriodFormModel]) => pages.reportingPeriod(header, form, companyDetail.companiesHouseId, df, serviceStartDate),
        (companyDetail: CompanyDetail) => routes.ReportingPeriodController.show(companyDetail.companiesHouseId)
      )
    case PaymentStatistics =>
      FormHandler(
        PaymentStatistics,
        emptyPaymentStatisticsForm,
        (header: Html, companyDetail: CompanyDetail) => (form: Form[PaymentStatisticsForm]) => pages.longFormPage1(header, form, companyDetail.companiesHouseId, df, serviceStartDate),
        (companyDetail: CompanyDetail) => routes.LongFormController.show(PaymentStatistics, companyDetail.companiesHouseId)
      )
    case PaymentTerms      =>
      FormHandler(
        PaymentTerms,
        emptyPaymentTermsForm,
        (header: Html, companyDetail: CompanyDetail) => (form: Form[PaymentTermsForm]) => pages.longFormPage2(header, form, companyDetail.companiesHouseId, df, serviceStartDate),
        (companyDetail: CompanyDetail) => routes.LongFormController.show(PaymentTerms, companyDetail.companiesHouseId)
      )
    case DisputeResolution =>
      FormHandler(
        DisputeResolution,
        emptyDisputeResolutionForm,
        (header: Html, companyDetail: CompanyDetail) => (form: Form[DisputeResolutionForm]) => pages.longFormPage3(header, form, companyDetail.companiesHouseId, df, serviceStartDate),
        (companyDetail: CompanyDetail) => routes.LongFormController.show(DisputeResolution, companyDetail.companiesHouseId)
      )
    case OtherInformation  =>
      FormHandler(
        OtherInformation,
        emptyOtherInformationForm,
        (header: Html, companyDetail: CompanyDetail) => (form: Form[OtherInformationForm]) => pages.longFormPage4(header, form, companyDetail.companiesHouseId, df, serviceStartDate),
        (companyDetail: CompanyDetail) => routes.LongFormController.show(OtherInformation, companyDetail.companiesHouseId)
      )
  }
}