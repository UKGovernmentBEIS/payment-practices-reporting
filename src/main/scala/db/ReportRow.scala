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

import models.{PaymentHistoryId, ReportId}
import org.joda.time.LocalDate

case class ReportRow(
                      id: ReportId,
                      companyId: String,
                      filingDate: LocalDate,
                      startDate: LocalDate,
                      endDate: LocalDate,
                      paymentTerms: String,
                      paymentPeriod: Int,
                      maximumContractPeriod: Int,
                      maximumContractPeriodComment: Option[String],
                      paymentTermsChangedComment: Option[String],
                      paymentTermsChangedNotifiedComment: Option[String],
                      paymentTermsComment: Option[String],
                      disputeResolution: String,
                      offerEInvoicing: Boolean,
                      offerSupplyChainFinance: Boolean,
                      retentionChargesInPolicy: Boolean,
                      retentionChargesInPast: Boolean,
                      paymentCodes: Option[String]
                    )

case class PaymentHistoryRow(
                              id: PaymentHistoryId,
                              reportId: ReportId,
                              averageDaysToPay: Int,
                              percentPaidLaterThanAgreedTerms: Int,
                              percentInvoicesWithin30Days: Int,
                              percentInvoicesWithin60Days: Int,
                              percentInvoicesBeyond60Days: Int
                            )