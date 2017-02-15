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

package views.html.helpers

import forms.report.{ReportFormModel, ReportReviewModel}
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.twirl.api.Html

import scala.language.implicitConversions

object ReviewTables {

  val df = DateTimeFormat.forPattern("d MMMM YYYY")

  type ReviewField = ReportReviewModel => Html
  type ReportField = ReportFormModel => Html

  def yesNo(b: Boolean): String = if (b) "Yes" else "No"

  implicit def stringToHtml(s: String): Html = Html(s)

  implicit def intToHtml(i: Int): Html = Html(i.toString)

  implicit def dateToHtml(d: LocalDate): Html = Html(d.toString)

  implicit def yesNoToHtml(b: Boolean): Html = Html(yesNo(b))

  implicit def optionToHtml(o: Option[String]): Html = Html(o.getOrElse(""))

  val topLevelInfo: Seq[(String, ReviewField)] = Seq(
    ("Filing date", _.report.filingDate)
  )

  val reportingDateRows: Seq[(String, ReportField)] = Seq(
    "Start date" -> (r => df.print(r.reportDates.startDate)),
    "End date" -> (r => df.print(r.reportDates.endDate))
  )

  val paymentHistoryRows: Seq[(String, ReportField)] = Seq(
    ("Average number of days until payment", _.paymentHistory.averageTimeToPay),
    ("Percentage of invoices paid later than agreed terms", _.paymentHistory.percentPaidLaterThanAgreedTerms),
    ("Percentage of invoices paid within 30 days", _.paymentHistory.percentageSplit.percentWithin30Days),
    ("Percentage of invoices paid within 31 to 60 days", _.paymentHistory.percentageSplit.percentWithin60Days),
    ("Percentage of invoices paid later than 60 days", _.paymentHistory.percentageSplit.percentBeyond60Days)
  )

  val paymentTermsRows: Seq[(String, ReportField)] = Seq(
    ("Payment terms", _.paymentTerms.terms),
    ("Maximum contract period", _.paymentTerms.maximumContractPeriod),
    ("Payment terms have changed", { r =>
      if (r.paymentTerms.paymentTermsChanged.yesNo) r.paymentTerms.paymentTermsChanged.text.map(text => s"<strong>Yes </strong>&ndash; $text")
      else "<strong>No</strong>"
    }),
    ("Suppliers notified of changes", { r =>
      if (r.paymentTerms.paymentTermsChangedNotified.yesNo) r.paymentTerms.paymentTermsChangedNotified.text.map(text => s"Yes &ndash; $text")
      else "<strong>No</strong>"
    }),
    ("Further remarks on payment terms", _.paymentTerms.paymentTermsComment),
    ("Dispute resolution", _.disputeResolution)
  )

  val otherInfoRows: Seq[(String, ReportField)] = Seq(
    ("Offer E-Invoicing", r => r.offerEInvoicing),
    ("Offer supply chain finance", r => r.offerSupplyChainFinancing),
    ("Retention charges covered in policy", r => r.retentionChargesInPolicy),
    ("Retention charges made in the past", r => r.retentionChargesInPast),
    ("Payment code participation", { r =>
      if (r.hasPaymentCodes.yesNo) r.hasPaymentCodes.text.map(text => s"Yes &ndash; $text")
      else "<strong>No</strong>"
    })
  )

}
