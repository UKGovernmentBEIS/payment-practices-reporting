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

package views.html.helpers

import javax.inject.Inject

import config.ServiceConfig
import controllers.FormPageDefs.MultiPageFormName
import controllers.routes
import play.api.mvc.Call
import services.CompanyDetail
import routes._

class FieldCallTable @Inject()(serviceConfig: ServiceConfig) {
  def call(fieldName: String)(implicit companyDetail: CompanyDetail): Option[Call] = fieldName match {
    case "reportDates.startDate" =>
      Some(ReportingPeriodController.show(companyDetail.companiesHouseId, Some(true)).withFragment(fieldName))
    case "reportDates.endDate"   =>
      Some(ReportingPeriodController.show(companyDetail.companiesHouseId, Some(true)).withFragment(fieldName))

    case _ if serviceConfig.multiPageForm =>
      multiPageCall(fieldName)

    case _ =>
      Some(SinglePageFormController.show(companyDetail.companiesHouseId, Some(true)).withFragment(fieldName))
  }

  def multiPageCall(fieldName: String)(implicit companyDetail: CompanyDetail): Option[Call] = fieldName match {
    case s if s.startsWith("paymentStatistics") =>
      Some(MultiPageFormController.show(MultiPageFormName.PaymentStatistics, companyDetail.companiesHouseId, Some(true)).withFragment(fieldName))
    case s if s.startsWith("paymentTerms")      =>
      Some(MultiPageFormController.show(MultiPageFormName.PaymentTerms, companyDetail.companiesHouseId, Some(true)).withFragment(fieldName))
    case "disputeResolution.text" =>
      Some(MultiPageFormController.show(MultiPageFormName.DisputeResolution, companyDetail.companiesHouseId, Some(true)).withFragment(fieldName))
    case s if s.startsWith("otherInformation")      =>
      Some(MultiPageFormController.show(MultiPageFormName.OtherInformation, companyDetail.companiesHouseId, Some(true)).withFragment(fieldName))

    case _                                      => None
  }
}
