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

import forms.report.{ConditionalText, ReportFormModel, ReportReviewModel}
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.twirl.api.{Html, HtmlFormat}

import scala.language.implicitConversions

/**
  * This object holds data that describes how the fields from the report are to be
  * grouped and displayed in tables on the review page.
  *
  * The top-level structure is returned by the `groups(companyName: String, r: ReportReviewModel)`
  * call. This returns a sequence of pairs where the first element is the css class name that
  * should be applied to the `<table>` tag and the second element is a sequence of mappings of
  * a string label to the Html value. These are turned into the `<tr>` elements in the table.
  */
object ReviewPageData extends HtmlHelpers {

  val df = DateTimeFormat.forPattern("d MMMM YYYY")

  /**
    * Maps a row label to the value for the row
    */
  type RowDescriptor = (String, Html)

  /**
    * Maps a css class (applied to the `<table>` tag) to a sequence of row descriptors
    */
  type TableDescriptor = (String, Seq[RowDescriptor])

  /**
    * Generate a sequence of pairs describing the `<table>`s that should be created on the
    * report review page. The first element is the css class name that should be applied to
    * the table and the second element is a sequence of row descriptors.
    *
    * The review page can be reconfigured by changing this list of tables or by changing
    * the content of the various groups.
    */
  def groups(companyName: String, r: ReportReviewModel): Seq[TableDescriptor] = Seq(
    "check-answers" -> group1(companyName, r),
    "check-answers check-answers-essay" -> group2(r.report),
    "check-answers" -> group3(r.report)
  )

  def group1(companyName: String, r: ReportReviewModel): Seq[RowDescriptor] =
    topLevelInfo(companyName, r) ++ reportingDateRows(r.report) ++ paymentHistoryRows(r.report)

  def group2(r: ReportFormModel) = paymentTermsRows(r)

  def group3(r: ReportFormModel) = otherInfoRows(r)

  def topLevelInfo(companyName: String, r: ReportReviewModel): Seq[(String, Html)] = Seq(
    ("Company", companyName),
    ("Filing date", r.report.filingDate)
  )

  def reportingDateRows(r: ReportFormModel): Seq[(String, Html)] = Seq(
    "Start date" -> df.print(r.reportDates.startDate),
    "End date" -> df.print(r.reportDates.endDate)
  )

  def paymentHistoryRows(r: ReportFormModel): Seq[(String, Html)] = Seq(
    ("Average number of days until payment", r.paymentHistory.averageTimeToPay),
    ("Percentage of invoices paid later than agreed terms", r.paymentHistory.percentPaidLaterThanAgreedTerms),
    ("Percentage of invoices paid within 30 days", r.paymentHistory.percentageSplit.percentWithin30Days),
    ("Percentage of invoices paid within 31 to 60 days", r.paymentHistory.percentageSplit.percentWithin60Days),
    ("Percentage of invoices paid later than 60 days", r.paymentHistory.percentageSplit.percentBeyond60Days)
  )

  def paymentTermsRows(r: ReportFormModel): Seq[(String, Html)] = Seq(
    ("Payment terms", r.paymentTerms.terms),
    ("Maximum contract period", breakLines(r.paymentTerms.maximumContractPeriod)),
    ("Payment terms have changed", conditionalText(r.paymentTerms.paymentTermsChanged)),
    ("Suppliers notified of changes", conditionalText(r.paymentTerms.paymentTermsChangedNotified)),
    ("Further remarks on payment terms", r.paymentTerms.paymentTermsComment.map(breakLines)),
    ("Dispute resolution", breakLines(r.disputeResolution))
  )

  def otherInfoRows(r: ReportFormModel): Seq[(String, Html)] = Seq(
    ("Offer E-Invoicing", r.offerEInvoicing),
    ("Offer supply chain finance", r.offerSupplyChainFinancing),
    ("Retention charges covered in policy", r.retentionChargesInPolicy),
    ("Retention charges made in the past", r.retentionChargesInPast),
    ("Payment code participation", conditionalText(r.hasPaymentCodes))
  )

}

/**
  * Various converters to reduce boilerplate in the table and row descriptors
  */
trait HtmlHelpers {
  def yesNo(b: Boolean): String = if (b) "Yes" else "No"

  def breakLines(s: String): Html = Html(HtmlFormat.escape(s).toString.replace("\n", "<br />"))

  def conditionalText(ct: ConditionalText): Html =
    if (ct.yesNo) Html(s"<strong>Yes </strong>&ndash; ${breakLines(ct.text.getOrElse(""))}")
    else Html("<strong>No</strong>")

  implicit def stringToHtml(s: String): Html = HtmlFormat.escape(s)

  implicit def intToHtml(i: Int): Html = Html(i.toString)

  implicit def dateToHtml(d: LocalDate): Html = Html(d.toString)

  implicit def yesNoToHtml(b: Boolean): Html = Html(yesNo(b))

  implicit def optionToHtml(o: Option[String]): Html = Html(o.getOrElse(""))

  implicit def optionHtmlToHtml(o: Option[Html]): Html = o.getOrElse(Html(""))
}