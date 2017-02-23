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

package forms.report

import forms.DateRange
import org.joda.time.LocalDate
import utils.YesNo
import utils.YesNo.{No, Yes}

object ReportConstants {
  val wordLength = 7
  val longTerms = wordLength * 5000
  val shortComment = wordLength * 500
  val longComment = wordLength * 2000
}


case class ConditionalText(yesNo: YesNo, text: Option[String]) {
  def normalize = this match {
    case ConditionalText(No, _) => ConditionalText(No, None)
    case ConditionalText(Yes, Some(t)) if t.trim() == "" => ConditionalText(Yes, None)
    case _ => this
  }
}

case class PercentageSplit(
                            percentWithin30Days: Int,
                            percentWithin60Days: Int,
                            percentBeyond60Days: Int
                          ) {
  def total: Int = percentWithin30Days + percentWithin60Days + percentBeyond60Days
}

case class PaymentHistory(
                           averageDaysToPay: Int,
                           percentPaidLaterThanAgreedTerms: Int,
                           percentageSplit: PercentageSplit
                         )


case class PaymentTerms(
                         terms: String,
                         paymentPeriod: Int,
                         maximumContractPeriod: Int,
                         maximumContractPeriodComment: Option[String],
                         paymentTermsChanged: ConditionalText,
                         paymentTermsChangedNotified: ConditionalText,
                         paymentTermsComment: Option[String],
                         disputeResolution: String
                       )

case class ReportFormModel(
                            reportDates: DateRange,
                            paymentHistory: PaymentHistory,
                            paymentTerms: PaymentTerms,
                            hasPaymentCodes: ConditionalText,
                            offerEInvoicing: YesNo,
                            offerSupplyChainFinancing: YesNo,
                            retentionChargesInPolicy: YesNo,
                            retentionChargesInPast: YesNo
                          )

case class ReportReviewModel(
                              confirmed: Boolean,
                              confirmedBy: String
                            )

