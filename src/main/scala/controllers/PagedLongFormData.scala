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

import forms.report._
import play.api.data.Forms._
import play.api.data.{Form, Mapping}

case class PaymentStatisticsForm(paymentStatistics: PaymentStatistics)
case class PaymentTermsForm(paymentTerms: PaymentTerms)
case class DisputeResolutionForm(disputeResolution: DisputeResolution)
case class OtherInformationForm(otherInformation: OtherInformation)

class PagedLongFormData @Inject()(validations: Validations) {

  import validations._

  val paymentStatisticsFormMapping: Mapping[PaymentStatisticsForm] = mapping(
    "paymentStatistics" -> paymentStatistics
  )(PaymentStatisticsForm.apply)(PaymentStatisticsForm.unapply)

  val paymentTermsFormMapping: Mapping[PaymentTermsForm] = mapping(
    "paymentTerms" -> paymentTerms
  )(PaymentTermsForm.apply)(PaymentTermsForm.unapply)

  val disputeResolutionFormMapping: Mapping[DisputeResolutionForm] = mapping(
    "disputeResolution" -> disputeResolution
  )(DisputeResolutionForm.apply)(DisputeResolutionForm.unapply)

  val otherInformationFormMapping: Mapping[OtherInformationForm] = mapping(
    "otherInformation" -> otherInformation
  )(OtherInformationForm.apply)(OtherInformationForm.unapply)

  val emptyPaymentStatisticsForm: Form[PaymentStatisticsForm] = Form(paymentStatisticsFormMapping)
  val emptyPaymentTermsForm     : Form[PaymentTermsForm]      = Form(paymentTermsFormMapping)
  val emptyDisputeResolutionForm: Form[DisputeResolutionForm] = Form(disputeResolutionFormMapping)
  val emptyOtherInformationForm : Form[OtherInformationForm]  = Form(otherInformationFormMapping)
}
