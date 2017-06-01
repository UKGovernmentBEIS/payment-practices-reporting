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

import dbrows.{ContractDetailsRow, ReportRow}
import forms.report._
import models.ReportId
import org.joda.time.LocalDate
import services.CompanyDetail

trait RowBuilders {

  def buildReport(companyDetail: CompanyDetail, review: ReportReviewModel, reportingPeriod: ReportingPeriodFormModel, paymentCodes: ConditionalText, confirmationEmail: String) = {
    ReportRow(
      ReportId(-1),
      companyDetail.companyName,
      companyDetail.companiesHouseId,
      LocalDate.now(),
      review.confirmedBy,
      confirmationEmail,
      reportingPeriod.reportDates.startDate,
      reportingPeriod.reportDates.endDate,
      paymentCodes.text
    )
  }

  def buildContractDetails(reportId: ReportId, longForm: LongFormModel): ContractDetailsRow =
    ContractDetailsRow(
      reportId,
      longForm.paymentTerms.terms,
      longForm.paymentTerms.paymentPeriod,
      longForm.paymentTerms.maximumContractPeriod,
      longForm.paymentTerms.maximumContractPeriodComment,
      longForm.paymentTerms.paymentTermsChanged.comment.text,
      longForm.paymentTerms.paymentTermsChanged.notified.flatMap(_.text),
      longForm.paymentTerms.paymentTermsComment,
      longForm.paymentTerms.disputeResolution,
      longForm.offerEInvoicing,
      longForm.offerSupplyChainFinancing,
      longForm.retentionChargesInPolicy,
      longForm.retentionChargesInPolicy,
      longForm.paymentHistory.averageDaysToPay,
      longForm.paymentHistory.percentPaidLaterThanAgreedTerms,
      longForm.paymentHistory.percentageSplit.percentWithin30Days,
      longForm.paymentHistory.percentageSplit.percentWithin60Days,
      longForm.paymentHistory.percentageSplit.percentBeyond60Days
    )
}
