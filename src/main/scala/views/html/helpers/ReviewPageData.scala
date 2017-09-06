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

import javax.inject.Inject

import controllers.routes
import forms.report.{ConditionalText, LongFormModel, ReportingPeriodFormModel, ShortFormModel}
import org.joda.time.LocalDate
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.mvc.Call
import play.twirl.api.{Html, HtmlFormat}
import services.CompanyDetail
import utils.YesNo
import utils.YesNo.{No, Yes}

import scala.language.implicitConversions

object ReviewPageData {
  /**
    * Maps a row label to the value for the row
    */
  type RowDescriptor = (String, Html, Option[Call])

  /**
    * Maps a css class (applied to the `<table>` tag) to a sequence of row descriptors
    */
  type TableDescriptor = (String, Seq[RowDescriptor])

}

/**
  * This object holds data that describes how the fields from the report are to be
  * grouped and displayed in tables on the review page.
  *
  * The top-level structure is returned by the `groups(companyName: String, r: ReportReviewModel)`
  * call. This returns a sequence of pairs where the first element is the css class name that
  * should be applied to the `<table>` tag and the second element is a sequence of mappings of
  * a string label to the Html value. These are turned into the `<tr>` elements in the table.
  */
class ReviewPageData @Inject()(fieldCallTable: FieldCallTable) extends HtmlHelpers {
  import ReviewPageData._


  val df: DateTimeFormatter = DateTimeFormat.forPattern("d MMMM YYYY")


  val cssClasses = "check-answers check-answers-essay"

  /**
    * Generate a sequence of pairs describing the `<table>`s that should be created on the
    * report review page. The first element is the css class name that should be applied to
    * the table and the second element is a sequence of row descriptors.
    *
    * The review page can be reconfigured by changing this list of tables or by changing
    * the content of the various groups.
    */
  def formGroups(reportingPeriod: ReportingPeriodFormModel, shortForm: ShortFormModel)(implicit companyDetail: CompanyDetail): Seq[TableDescriptor] = {
    Seq(
      cssClasses -> (group1(reportingPeriod) ++ group3(shortForm))
    )
  }

  def formGroups(reportingPeriod: ReportingPeriodFormModel, longForm: LongFormModel)(implicit companyDetail: CompanyDetail): Seq[TableDescriptor] = {
    Seq(
      cssClasses -> (group1(reportingPeriod) ++ group2(longForm) ++ group3(longForm))
    )
  }

  def group1(reportingPeriod: ReportingPeriodFormModel)(implicit companyDetail: CompanyDetail): Seq[RowDescriptor] =
    topLevelInfo(companyDetail.companyName) ++ reportingDateRows(reportingPeriod)

  def group2(longForm: LongFormModel)(implicit companyDetail: CompanyDetail): Seq[RowDescriptor] =
    paymentStatisticsRows(longForm) ++ paymentTermsRows(longForm) ++ disputeResolutionRows(longForm)

  def group3(shortForm: ShortFormModel)(implicit companyDetail: CompanyDetail): Seq[RowDescriptor] =
    paymentCodesRows(shortForm)

  def group3(longForm: LongFormModel)(implicit companyDetail: CompanyDetail): Seq[RowDescriptor] =
    otherInfoRows(longForm)

  import fieldCallTable.call

  def topLevelInfo(companyName: String): Seq[RowDescriptor] = Seq(
    ("Company or limited liability partnership", companyName, None)
  )

  def reportingDateRows(r: ReportingPeriodFormModel)(implicit companyDetail: CompanyDetail): Seq[RowDescriptor] = Seq(
    ("Start date of reporting period", df.print(r.reportDates.startDate), call("reportDates.startDate")),
    ("End date of reporting period", df.print(r.reportDates.endDate), call("reportDates.endDate"))
  )

  private val percent = "%"
  private val days = "days"

  def paymentStatisticsRows(r: LongFormModel)(implicit companyDetail: CompanyDetail): Seq[RowDescriptor] = {
    Seq(
      ("Average number of " + days + " for making payment", (r.paymentStatistics.averageDaysToPay, days), call("paymentStatistics.averageDaysToPay")),
      ("Percentage of invoices paid within 30 " + days, (r.paymentStatistics.percentageSplit.percentWithin30Days, percent), call("paymentStatistics.percentageSplit.percentWithin30Days")),
      ("Percentage of invoices paid within 31 to 60 " + days, (r.paymentStatistics.percentageSplit.percentWithin60Days, percent), call("paymentStatistics.percentageSplit.percentWithin60Days")),
      ("Percentage of invoices paid on or after day 61", (r.paymentStatistics.percentageSplit.percentBeyond60Days, percent), call("paymentStatistics.percentageSplit.percentBeyond60Days")),
      ("Percentage of invoices not paid within agreed terms", (r.paymentStatistics.percentPaidLaterThanAgreedTerms, percent), call("paymentStatistics.percentPaidLaterThanAgreedTerms"))
    )
  }

  def paymentTermsRows(r: LongFormModel)(implicit companyDetail: CompanyDetail): Seq[RowDescriptor] = Seq(
    ("Shortest standard payment period", (r.paymentTerms.shortestPaymentPeriod, days), call("paymentTerms.shortestPaymentPeriod")),
    ("Longest standard payment period", (r.paymentTerms.longestPaymentPeriod, days), call("paymentTerms.longestPaymentPeriod")),
    ("Standard payment terms", r.paymentTerms.terms, call("paymentTerms.terms")),
    ("Any changes to standard payment terms", r.paymentTerms.paymentTermsChanged.comment, call("paymentTerms.paymentTermsChanged.changed.yesNo")),
    ("Did you consult or notify your suppliers about changes?",
      if (r.paymentTerms.paymentTermsChanged.notified.exists(_.yesNo.toBoolean)) r.paymentTerms.paymentTermsChanged.notified.map(conditionalText)
      else "N/A",
      if (r.paymentTerms.paymentTermsChanged.notified.exists(_.yesNo.toBoolean)) call("paymentTerms.paymentTermsChanged.notified.yesNo")
      else None
    ),
    ("Maximum contract period in " + days, (r.paymentTerms.maximumContractPeriod, days), call("paymentTerms.maximumContractPeriod")),
    ("Maximum contract period: further information", r.paymentTerms.maximumContractPeriodComment.map(breakLines), call("paymentTerms.maximumContractPeriodComment")),
    ("Further remarks about your payment terms", r.paymentTerms.paymentTermsComment.map(breakLines), call("paymentTerms.paymentTermsComment"))
  )

  def disputeResolutionRows(r: LongFormModel)(implicit companyDetail: CompanyDetail): Seq[RowDescriptor] = Seq(
    ("Your dispute resolution process", breakLines(r.disputeResolution.text), call("disputeResolution.text"))
  )

  private val codeOfConductText = "Are you a member of a code of conduct or standards on payment practices?"

  def otherInfoRows(longForm: LongFormModel)(implicit companyDetail: CompanyDetail): Seq[RowDescriptor] = Seq(
    ("Do you offer e-invoicing?", longForm.otherInformation.offerEInvoicing, call("otherInformation.offerEInvoicing")),
    ("Do you offer offer supply chain finance?", longForm.otherInformation.offerSupplyChainFinance, call("otherInformation.offerSupplyChainFinance")),
    ("Do you have a policy of deducting sums from payments under qualifying contracts as a charge for remaining on a supplier list?",
      longForm.otherInformation.retentionChargesInPolicy, call("otherInformation.retentionChargesInPolicy")),
    ("In this reporting period, have you deducted any sum from payments under qualifying contracts as a charge for remaining on a supplier list?",
      longForm.otherInformation.retentionChargesInPast, call("otherInformation.retentionChargesInPast")),
    (codeOfConductText, longForm.otherInformation.paymentCodes, call("otherInformation.paymentCodes.yesNo"))
  )

  def paymentCodesRows(shortForm: ShortFormModel)(implicit companyDetail: CompanyDetail): Seq[RowDescriptor] = Seq(
    (codeOfConductText, shortForm.paymentCodes, Some(routes.ShortFormController.show(companyDetail.companiesHouseId, Some(true)).withFragment("paymentCodes.yesNo")))
  )

}

/**
  * Various converters to reduce boilerplate in the table and row descriptors
  */
trait HtmlHelpers {
  def limitLength(s: String, maxLength: Int = 1000): String =
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