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

import forms.report.{ConditionalText, LongFormModel, ReportingPeriodFormModel, ShortFormModel}
import org.joda.time.LocalDate
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.twirl.api.{Html, HtmlFormat}
import utils.YesNo
import utils.YesNo.{No, Yes}

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

  val df: DateTimeFormatter = DateTimeFormat.forPattern("d MMMM YYYY")

  /**
    * Maps a row label to the value for the row
    */
  type RowDescriptor = (String, Html)

  /**
    * Maps a css class (applied to the `<table>` tag) to a sequence of row descriptors
    */
  type TableDescriptor = (String, Seq[RowDescriptor])

  val cssClasses = "check-answers check-answers-essay"

  /**
    * Generate a sequence of pairs describing the `<table>`s that should be created on the
    * report review page. The first element is the css class name that should be applied to
    * the table and the second element is a sequence of row descriptors.
    *
    * The review page can be reconfigured by changing this list of tables or by changing
    * the content of the various groups.
    */
  def formGroups(companyName: String, reportingPeriod: ReportingPeriodFormModel, shortForm: ShortFormModel): Seq[TableDescriptor] = {
    Seq(
      cssClasses -> group1(companyName, reportingPeriod),
      cssClasses -> group3(shortForm)
    )
  }

  def formGroups(companyName: String, reportingPeriod: ReportingPeriodFormModel, longForm: LongFormModel): Seq[TableDescriptor] = {
    Seq(
      cssClasses -> group1(companyName, reportingPeriod, longForm),
      cssClasses -> group2(longForm),
      cssClasses -> group3(longForm)
    )
  }

  def group1(companyName: String, reportingPeriod: ReportingPeriodFormModel, longForm: LongFormModel): Seq[RowDescriptor] =
    topLevelInfo(companyName) ++ reportingDateRows(reportingPeriod) ++ paymentStatisticsRows(longForm)

  def group1(companyName: String, reportingPeriod: ReportingPeriodFormModel): Seq[RowDescriptor] =
    topLevelInfo(companyName) ++ reportingDateRows(reportingPeriod)

  def group2(r: LongFormModel): Seq[(String, Html)] = paymentTermsRows(r)

  def group3(shortForm: ShortFormModel): Seq[(String, Html)] = paymentCodesRows(shortForm)

  def group3(longForm: LongFormModel): Seq[(String, Html)] = otherInfoRows(longForm)

  def topLevelInfo(companyName: String): Seq[RowDescriptor] = Seq(
    ("Company or limited liability partnership", companyName)
  )

  def reportingDateRows(r: ReportingPeriodFormModel): Seq[RowDescriptor] = Seq(
    "Start date of reporting period" -> df.print(r.reportDates.startDate),
    "End date of reporting period" -> df.print(r.reportDates.endDate)
  )

  def paymentStatisticsRows(r: LongFormModel): Seq[RowDescriptor] = Seq(
    ("Average number of days for making payment", (r.paymentStatistics.averageDaysToPay, "days")),
    ("Percentage of invoices paid within 30 days", (r.paymentStatistics.percentageSplit.percentWithin30Days, "%")),
    ("Percentage of invoices paid within 31 to 60 days", (r.paymentStatistics.percentageSplit.percentWithin60Days, "%")),
    ("Percentage of invoices paid on or after day 61", (r.paymentStatistics.percentageSplit.percentBeyond60Days, "%")),
    ("Percentage of invoices not paid within agreed terms", (r.paymentStatistics.percentPaidLaterThanAgreedTerms, "%"))
  )

  def paymentTermsRows(r: LongFormModel): Seq[RowDescriptor] = Seq(
    ("Shortest standard payment period", (r.paymentTerms.shortestPaymentPeriod, "days")),
    ("Longest standard payment period", (r.paymentTerms.longestPaymentPeriod, "days")),
    ("Standard payment terms", r.paymentTerms.terms),
    ("Any changes to standard payment terms", r.paymentTerms.paymentTermsChanged.comment),
    ("Did you consult or notify your suppliers about changes?",
      r.paymentTerms.paymentTermsChanged.notified.map(conditionalText)),
    ("Maximum contract period in days", (r.paymentTerms.maximumContractPeriod, "days")),
    ("Maximum contract period: further information", r.paymentTerms.maximumContractPeriodComment.map(breakLines)),
    ("Further remarks about your payment terms", r.paymentTerms.paymentTermsComment.map(breakLines)),
    ("Your dispute resolution process", breakLines(r.disputeResolution.text))
  )

  private val codeOfConductText = "Are you a member of a code of conduct or standards on payment practices?"

  def otherInfoRows(longForm: LongFormModel): Seq[RowDescriptor] = Seq(
    ("Do you offer e-invoicing?", longForm.otherInformation.offerEInvoicing),
    ("Do you offer offer supply chain finance?", longForm.otherInformation.offerSupplyChainFinance),
    ("Do you have a policy of deducting sums from payments under qualifying contracts as a charge for remaining on a supplier list?", longForm.otherInformation.retentionChargesInPolicy),
    ("In this reporting period, have you deducted any sum from payments under qualifying contracts as a charge for remaining on a supplier list?", longForm.otherInformation.retentionChargesInPast),
    (codeOfConductText, longForm.otherInformation.paymentCodes)
  )

  def paymentCodesRows(shortForm: ShortFormModel): Seq[RowDescriptor] = Seq(
    (codeOfConductText, shortForm.paymentCodes)
  )

}

/**
  * Various converters to reduce boilerplate in the table and row descriptors
  */
trait HtmlHelpers {
  def limitLength(s: String, maxLength: Int = 1000) =
    if (s.length <= maxLength) s else s"${s.take(maxLength)}..."

  def yesNo(yn: YesNo): String = yn.entryName.capitalize

  def breakLines(s: String): Html = Html(HtmlFormat.escape(limitLength(s)).toString.replace("\n", "<br />"))

  implicit def conditionalText(ct: ConditionalText): Html = ct.yesNo match {
    case Yes => Html(s"<strong>Yes </strong>&ndash; ${breakLines(ct.text.map(limitLength(_)).getOrElse(""))}")
    case No  => Html("<strong>No</strong>")
  }

  implicit def stringToHtml(s: String): Html = HtmlFormat.escape(limitLength(s))

  implicit def intToHtml(i: Int): Html = Html(i.toString)

  /**
    * Slightly hacky. Take a pair with an int and a string that contains the units (e.g. "days" or "%")
    * and format them up. would be better to have strong types representing days and percentages.
    */
  implicit def unitsToHtml(p: (Int, String)): Html = Html(s"${p._1} ${p._2}")
  implicit def optUnitsToHtml(p: (Option[Int], String)): Html = p._1.map(i => Html(s"$i ${p._2}")).getOrElse(Html(""))

  implicit def dateToHtml(d: LocalDate): Html = Html(d.toString)

  implicit def yesNoToHtml(yn: YesNo): Html = Html(yesNo(yn))

  implicit def optionToHtml(o: Option[String]): Html = Html(o.map(limitLength(_)).getOrElse(""))
  implicit def optionIntToHtml(o: Option[Int]): Html = Html(o.map(_.toString).getOrElse(""))

  implicit def optionHtmlToHtml(o: Option[Html]): Html = o.getOrElse(Html(""))
}

object HtmlHelpers extends HtmlHelpers