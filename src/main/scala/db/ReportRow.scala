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

package db

import models.{CompaniesHouseId, ReportId}
import org.joda.time.LocalDate
import utils.YesNo

/**
  * The master record for a report. Created when the user starts filling out the form.
  * If the user does not complete the form within a limited time period then the report
  * may be purged from the database.
  */
case class ReportHeaderRow(
                            id: ReportId,
                            companyName: String,
                            companyId: CompaniesHouseId,
                            createdAt: LocalDate,
                            updatedAt: LocalDate
                          )

/**
  * Created when the users reviews and confirms the report. Once a FilingRow record is
  * created the report is considered to have been filed. Up until that point the form
  * is un-filed and provisional.
  */
case class FilingRow(
                      reportId: ReportId,
                      filingDate: LocalDate,
                      approvedBy: String,
                      confirmationEmailAddress: String
                    )

case class ReportPeriodRow(
                            reportId: ReportId,
                            startDate: LocalDate,
                            endDate: LocalDate
                          )

case class PaymentTermsRow(
                            reportId: ReportId,
                            paymentTerms: String,
                            paymentPeriod: Int,
                            maximumContractPeriod: Int,
                            maximumContractPeriodComment: Option[String],
                            paymentTermsChangedComment: Option[String],
                            paymentTermsChangedNotifiedComment: Option[String],
                            paymentTermsComment: Option[String],
                            disputeResolution: String
                          )

case class OtherInfoRow(
                         reportId: ReportId,
                         offerEInvoicing: YesNo,
                         offerSupplyChainFinance: YesNo,
                         retentionChargesInPolicy: YesNo,
                         retentionChargesInPast: YesNo,
                         paymentCodes: Option[String]
                       )

case class PaymentHistoryRow(
                              reportId: ReportId,
                              averageDaysToPay: Int,
                              percentPaidLaterThanAgreedTerms: Int,
                              percentInvoicesWithin30Days: Int,
                              percentInvoicesWithin60Days: Int,
                              percentInvoicesBeyond60Days: Int
                            )