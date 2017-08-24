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

import dbrows.ContractDetailsRow
import forms.DateRange
import org.scalactic.TripleEquals._
import services.ContractDetails
import utils.YesNo
import utils.YesNo.{No, Yes}

object ReportConstants {
  /**
    * We need to translate word counts into character counts for the database. This constant
    * defines the average word length (including whitespace)
    */
  val averageWordLength: Int = 7

  val longTerms   : Int = 5000
  val longComment : Int = 2000
  val shortComment: Int = 500

  val paymentTermsWordCount: Int = longTerms
  val paymentTermsCharCount: Int = paymentTermsWordCount * averageWordLength

  val maxContractPeriodCommentWordCount: Int = shortComment
  val maxContractPeriodCommentCharCount: Int = maxContractPeriodCommentWordCount * averageWordLength

  val paymentTermsCommentWordCount: Int = shortComment
  val paymentTermsCommentCharCount: Int = paymentTermsCommentWordCount * averageWordLength

  val disputeResolutionWordCount: Int = longComment
  val disputeResolutionCharCount: Int = disputeResolutionWordCount * averageWordLength

  val paymentTermsChangedWordCount: Int = shortComment
  val paymentTermsChangedCharCount: Int = paymentTermsChangedWordCount * averageWordLength

  val paymentTermsNotifiedWordCount: Int = shortComment
  val paymentTermsNotifiedCharCount: Int = paymentTermsNotifiedWordCount * averageWordLength

  val paymentCodesWordCount: Int = 35
  val paymentCodesCharCount: Int = paymentCodesWordCount * averageWordLength
}

case class ConditionalText(yesNo: YesNo, text: Option[String]) {
  def normalize: ConditionalText = this match {
    case ConditionalText(No, _)                           => ConditionalText(No, None)
    case ConditionalText(Yes, Some(t)) if t.trim() === "" => ConditionalText(Yes, None)
    case _                                                => this
  }

  def isDefined: Boolean = yesNo.toBoolean
}

object ConditionalText {
  def apply(o: Option[String]): ConditionalText =
    o.map(s => ConditionalText(Yes, Some(s))).getOrElse(ConditionalText(No, None))

  def apply(s: String): ConditionalText = ConditionalText(Some(s))
}

case class PercentageSplit(
  percentWithin30Days: Int,
  percentWithin60Days: Int,
  percentBeyond60Days: Int
) {
  def total: Int = percentWithin30Days + percentWithin60Days + percentBeyond60Days
}

case class PaymentStatistics(
  averageDaysToPay: Int,
  percentageSplit: PercentageSplit,
  percentPaidLaterThanAgreedTerms: Int
)

object PaymentStatistics {
  def apply(row: ContractDetailsRow): PaymentStatistics =
    PaymentStatistics(
      row.averageDaysToPay,
      PercentageSplit(row.percentInvoicesWithin30Days, row.percentInvoicesWithin60Days, row.percentInvoicesBeyond60Days),
      row.percentPaidLaterThanAgreedTerms
    )
}

case class PaymentTermsChanged(comment: ConditionalText, notified: Option[ConditionalText]) {
  /**
    * If the answer to the comment question is No then remove any answer to the Notified question
    */
  def normalise: PaymentTermsChanged = this match {
    case PaymentTermsChanged(c@ConditionalText(No, _), _) => PaymentTermsChanged(c, None)
    case _                                                => this
  }
}

case class PaymentTerms(
  shortestPaymentPeriod: Int,
  longestPaymentPeriod: Option[Int],
  terms: String,
  maximumContractPeriod: Int,
  maximumContractPeriodComment: Option[String],
  paymentTermsChanged: PaymentTermsChanged,
  paymentTermsComment: Option[String]
)

case class DisputeResolution(disputeResolutionText: String)

case class OtherInformation(
  offerEInvoicing: YesNo,
  offerSupplyChainFinance: YesNo,
  retentionChargesInPolicy: YesNo,
  retentionChargesInPast: YesNo,
  paymentCodes: ConditionalText
)

object PaymentTerms {
  def apply(row: ContractDetailsRow): PaymentTerms =
    PaymentTerms(
      row.shortestPaymentPeriod, row.longestPaymentPeriod, row.paymentTerms, row.maximumContractPeriod, row.maximumContractPeriodComment,
      pt(row),
      row.paymentTermsChangedComment
    )

  def pt(row: ContractDetailsRow): PaymentTermsChanged = {
    val comment = ConditionalText(row.paymentTermsChangedComment)
    val notified =
      if (comment.yesNo === Yes) Some(ConditionalText(row.paymentTermsChangedNotifiedComment))
      else None

    PaymentTermsChanged(comment, notified)
  }
}

case class ReportingPeriodFormModel(
  reportDates: DateRange,
  hasQualifyingContracts: YesNo
)

case class ShortFormModel(
  paymentCodes: ConditionalText
)

case class LongFormModel(
  paymentStatistics: PaymentStatistics,
  paymentTerms: PaymentTerms,
  disputeResolution: DisputeResolution,
  otherInformation: OtherInformation
)

object LongFormModel {
  def apply(paymentCodes: ConditionalText, report: ContractDetails): LongFormModel = {
    LongFormModel(
      report.paymentStatistics,
      report.paymentTerms,
      report.disputeResolution,
      report.otherInformation)
  }

}

case class ReportReviewModel(
  confirmedBy: String,
  confirmed: Boolean
)



