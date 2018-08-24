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
import controllers.FormPageDefs.MultiPageFormName._
import controllers.FormPageDefs.{MultiPageFormHandler, MultiPageFormName}
import forms.report.{ReportingPeriodFormModel, Validations}
import models.CompaniesHouseId
import org.joda.time.format.DateTimeFormat
import play.api.data.Forms.mapping
import play.api.data.{Form, Mapping}
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsBoolean, JsObject, JsString}
import play.twirl.api.Html
import services._

import scala.concurrent.Future


case class PaymentStatisticsForm(paymentStatistics: forms.report.PaymentStatistics)
case class PaymentTermsForm(paymentTerms: forms.report.PaymentTerms)
case class DisputeResolutionForm(disputeResolution: forms.report.DisputeResolution)
case class OtherInformationForm(otherInformation: forms.report.OtherInformation)

class MultiPageFormPageModel @Inject()(validations: Validations, serviceConfig: ServiceConfig)(implicit messagesApi: MessagesApi)
  extends FormPageModel[MultiPageFormHandler[_], MultiPageFormName]
    with FormPageHelpers[MultiPageFormHandler[_], MultiPageFormName] {

  import validations._
  import views.html.{report => pages}

  private val df = DateTimeFormat.forPattern("d MMMM YYYY")

  private val serviceStartDate = serviceConfig.startDate.getOrElse(ServiceConfig.defaultServiceStartDate)

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

  override def formNames: Seq[MultiPageFormName] = MultiPageFormName.values

  override def handlerFor(formName: MultiPageFormName): MultiPageFormHandler[_] = formName match {
    case ReportingPeriod   =>
      FormHandler(
        ReportingPeriod,
        emptyReportingPeriod,
        (header: Html, companiesHouseId: CompaniesHouseId, change: Boolean, session: Option[Future[JsObject]]) => (form: Form[ReportingPeriodFormModel]) =>
          pages.reportingPeriod(header, form, companiesHouseId, df, serviceStartDate, if (change) Some(true) else None),
        (companiesHouseId: CompaniesHouseId, change: Boolean) =>
          routes.ReportingPeriodController.show(companiesHouseId, if (change) Some(true) else None)
      )
    case PaymentStatistics =>
      FormHandler(
        PaymentStatistics,
        emptyPaymentStatisticsForm,
        (header: Html, companiesHouseId: CompaniesHouseId, change: Boolean, session: Option[Future[JsObject]]) =>
          (form: Form[PaymentStatisticsForm]) => {
            pages.paymentStatisticsForm(header, form, session.get, companiesHouseId, df, serviceStartDate, if (change) Some(true) else None)
          },
        (companiesHouseId: CompaniesHouseId, change: Boolean) =>
          routes.MultiPageFormController.show(PaymentStatistics, companiesHouseId, if (change) Some(true) else None)
      )
    case PaymentTerms      =>
      FormHandler(
        PaymentTerms,
        emptyPaymentTermsForm,
        (header: Html, companiesHouseId: CompaniesHouseId, change: Boolean, session: Option[Future[JsObject]]) =>
          (form: Form[PaymentTermsForm]) => pages.paymentTermsForm(header, form, companiesHouseId, df, serviceStartDate, if (change) Some(true) else None),
        (companiesHouseId: CompaniesHouseId, change: Boolean) =>
          routes.MultiPageFormController.show(PaymentTerms, companiesHouseId, if (change) Some(true) else None)
      )
    case DisputeResolution =>
      FormHandler(
        DisputeResolution,
        emptyDisputeResolutionForm,
        (header: Html, companiesHouseId: CompaniesHouseId, change: Boolean, session: Option[Future[JsObject]]) =>
          (form: Form[DisputeResolutionForm]) => pages.disputeResolutionForm(header, form, companiesHouseId, df, serviceStartDate, if (change) Some(true) else None),
        (companiesHouseId: CompaniesHouseId, change: Boolean) =>
          routes.MultiPageFormController.show(DisputeResolution, companiesHouseId, if (change) Some(true) else None)
      )
    case OtherInformation  =>
      FormHandler(
        OtherInformation,
        emptyOtherInformationForm,
        (header: Html, companiesHouseId: CompaniesHouseId, change: Boolean, session: Option[Future[JsObject]]) =>
          (form: Form[OtherInformationForm]) => pages.otherInformationForm(header, form, companiesHouseId, df, serviceStartDate, if (change) Some(true) else None),
        (companiesHouseId: CompaniesHouseId, change: Boolean) =>
          routes.MultiPageFormController.show(OtherInformation, companiesHouseId, if (change) Some(true) else None)
      )
  }
}