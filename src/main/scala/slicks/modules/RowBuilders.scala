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
