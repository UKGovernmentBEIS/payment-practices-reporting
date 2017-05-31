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

package slicks.helpers

import dbrows.{LongFormRow, ShortFormRow}
import forms.report.{LongFormModel, PaymentCodesFormModel, ReportReviewModel, ReportingPeriodFormModel}
import models.ReportId
import org.joda.time.LocalDate
import services.{CompanyDetail, LongForm}

trait RowBuilders {

  def buildShortFormRow(companyDetail: CompanyDetail, review: ReportReviewModel, reportingPeriod: ReportingPeriodFormModel, paymentCodesFormModel: PaymentCodesFormModel, confirmationEmail: String) = {
    ShortFormRow(
      ReportId(-1),
      companyDetail.companyName,
      companyDetail.companiesHouseId,
      LocalDate.now(),
      review.confirmedBy,
      confirmationEmail,
      reportingPeriod.reportDates.startDate,
      reportingPeriod.reportDates.endDate,
      paymentCodesFormModel.paymentCodes.text
    )
  }

  def buildLongFormRow(reportId: ReportId, longForm: LongFormModel) :LongFormRow = ???
}
