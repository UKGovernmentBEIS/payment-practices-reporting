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

package services

import com.google.inject.ImplementedBy
import dbrows.{ContractDetailsRow, ReportRow}
import forms.DateRange
import forms.report._
import models.{CompaniesHouseId, ReportId}
import org.joda.time.LocalDate
import org.reactivestreams.Publisher
import slicks.repos.ReportTable

import scala.concurrent.Future

case class Report(
  id: ReportId,
  companyName: String,
  companyId: CompaniesHouseId,
  filingDate: LocalDate,

  approvedBy: String,
  confirmationEmailAddress: String,

  reportDates: DateRange,
  paymentCodes: ConditionalText,

  contractDetails: Option[ContractDetails]
)

object Report {
  def apply(r: (ReportRow, Option[ContractDetailsRow])): Report = {
    val (reportRow, contractDetailsRow) = r
    import reportRow._
    Report(
      id,
      companyName,
      companyId,
      filingDate,
      approvedBy,
      confirmationEmailAddress,
      DateRange(startDate, endDate),
      ConditionalText(paymentCodes),
      contractDetailsRow.map(buildContractDetails(reportRow, _))
    )
  }

  def buildContractDetails(report: ReportRow, longForm: ContractDetailsRow): ContractDetails = {
    import longForm._
    ContractDetails(
      PaymentHistory(
        averageDaysToPay,
        PercentageSplit(percentInvoicesWithin30Days, percentInvoicesWithin60Days, percentInvoicesBeyond60Days),
        percentPaidLaterThanAgreedTerms
      ),
      PaymentTerms(
        shortestPaymentPeriod,
        longestPaymentPeriod,
        paymentTerms,
        maximumContractPeriod,
        maximumContractPeriodComment,
        PaymentTermsChanged(ConditionalText(paymentTermsChangedComment), Some(ConditionalText(paymentTermsChangedNotifiedComment))).normalise,
        paymentTermsComment
      ),
      DisputeResolution(disputeResolution),
      OtherInformation(
        offerEInvoicing,
        offerSupplyChainFinance,
        retentionChargesInPolicy,
        retentionChargesInPast,
        ConditionalText(report.paymentCodes)
      )
    )
  }
}

case class ContractDetails(
  paymentHistory: PaymentHistory,
  paymentTerms: PaymentTerms,
  disputeResolution: DisputeResolution,
  otherInformation: OtherInformation
)


@ImplementedBy(classOf[ReportTable])
trait ReportService {
  def find(id: ReportId): Future[Option[Report]]

  def byCompanyNumber(companiesHouseId: CompaniesHouseId): Future[Seq[Report]]

  def list(cutoffDate: LocalDate): Publisher[Report]

  /**
    *
    * @param reportUrl - because we won't know what the `reportId` is until the table row is created,
    *                  and because we don't want to pass a Play request all the way down into this
    *                  module, the `reportUrl` parameter is a function that accepts a `reportId` and
    *                  generates the absolute url for it.
    */
  def create(
    companyDetail: CompanyDetail,
    reportingPeriod: ReportingPeriodFormModel,
    longForm: LongFormModel,
    review: ReportReviewModel,
    confirmationEmailAddress: String,
    reportUrl: (ReportId) => String
  ): Future[ReportId]

  def create(
    companyDetail: CompanyDetail,
    reportingPeriod: ReportingPeriodFormModel,
    shortFormModel: ShortFormModel,
    review: ReportReviewModel,
    confirmationEmailAddress: String,
    reportUrl: (ReportId) => String
  ): Future[ReportId]
}
