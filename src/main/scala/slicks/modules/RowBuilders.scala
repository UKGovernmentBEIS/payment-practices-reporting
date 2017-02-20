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

package slicks.modules

import db._
import forms.report.{ReportFormModel, ReportReviewModel}
import models.ReportId
import org.joda.time.LocalDate

trait RowBuilders {
  def buildPeriodRow(report: ReportFormModel, reportId: ReportId) = ReportPeriodRow(reportId, report.reportDates.startDate, report.reportDates.endDate)

  def buildPaymentTermsRow(report: ReportFormModel, reportId: ReportId) =
    PaymentTermsRow(
      reportId,
      report.paymentTerms.terms,
      report.paymentTerms.paymentPeriod,
      report.paymentTerms.maximumContractPeriod,
      report.paymentTerms.maximumContractPeriodComment,
      report.paymentTerms.paymentTermsChanged.text,
      report.paymentTerms.paymentTermsChangedNotified.text,
      report.paymentTerms.paymentTermsComment,
      report.paymentTerms.disputeResolution
    )

  def buildPaymentHistoryRow(report: ReportFormModel, reportId: ReportId) = PaymentHistoryRow(
    reportId,
    report.paymentHistory.averageDaysToPay,
    report.paymentHistory.percentPaidLaterThanAgreedTerms,
    report.paymentHistory.percentageSplit.percentWithin30Days,
    report.paymentHistory.percentageSplit.percentWithin60Days,
    report.paymentHistory.percentageSplit.percentBeyond60Days
  )

  def buildOtherInfoRow(report: ReportFormModel, reportId: ReportId): OtherInfoRow =
    OtherInfoRow(
      reportId,
      report.offerEInvoicing,
      report.offerSupplyChainFinancing,
      report.retentionChargesInPolicy,
      report.retentionChargesInPast,
      report.hasPaymentCodes.text
    )

  def buildFilingRow(review: ReportReviewModel, reportId: ReportId): FilingRow =
    FilingRow(reportId, new LocalDate(), review.confirmedBy)
}
