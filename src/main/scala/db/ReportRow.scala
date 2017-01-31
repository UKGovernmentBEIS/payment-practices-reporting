/*
 * Copyright (C) 2016  Department for Business, Energy and Industrial Strategy
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

import org.joda.time.LocalDate

case class ReportId(id: Long) extends AnyVal

case class ReportRow(
                      id: ReportId,
                      companyId: CompanyId,
                      filingDate: LocalDate,
                      averageDaysToPay: Int,
                      percentInvoicesPaidBeyondAgreedTerms: BigDecimal,
                      percentInvoicesWithin30Days: BigDecimal,
                      percentInvoicesWithin60Days: BigDecimal,
                      percentInvoicesBeyond60Days: BigDecimal,
                      startDate: LocalDate,
                      endDate: LocalDate,
                      paymentTerms: String,
                      maximumContractPeriod: String,
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
