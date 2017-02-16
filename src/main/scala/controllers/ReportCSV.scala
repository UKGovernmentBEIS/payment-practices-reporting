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

package controllers

import org.joda.time.LocalDate
import slicks.modules.CompanyReport

import scala.language.implicitConversions

object ReportCSV {

  case class CSVString(s: String)

  val charsThatNeedQuoting = Seq(',', '\n', '\r')
  val charsThatNeedDoubling = Seq('"')

  def quote(s: String): String = s""""$s""""

  def escape(s: String): String = s match {
    case _ if charsThatNeedDoubling.exists(s.contains(_)) => quote(charsThatNeedDoubling.foldLeft(s) { case (t, c) => t.replace(s"$c", s"$c$c") })
    case _ if charsThatNeedQuoting.exists(s.contains(_)) => quote(s)
    case _ => s
  }

  implicit def stringToCSVString(s: String): CSVString = CSVString(escape(s))

  implicit def intToCSVString(i: Int): CSVString = CSVString(i.toString)

  implicit def decimalToCSVString(d: BigDecimal): CSVString = CSVString(d.toString)

  implicit def dateToCSVString(d: LocalDate): CSVString = CSVString(d.toString)

  implicit def booleanToCSVString(b: Boolean): CSVString = CSVString(b.toString)

  implicit def optionToCSVString(o: Option[String]): CSVString = o.map(stringToCSVString).getOrElse(CSVString(""))

  def columns = Seq[(String, CompanyReport => CSVString)](
    ("Start date", _.report.startDate),
    ("End date", _.report.endDate),
    ("Filing date", _.report.filingDate),
    ("Company", _.name),
    ("Company number", _.report.companyId),
//    ("Average time to pay", _.report.averageDaysToPay),
//    ("% Invoices paid late", _.report.percentInvoicesPaidBeyondAgreedTerms),
//    ("% Invoices paid within 30 days", _.report.percentInvoicesWithin30Days),
//    ("% Invoices paid within 60 days", _.report.percentInvoicesWithin60Days),
//    ("% Invoices paid later than 60 days", _.report.percentInvoicesBeyond60Days),
    ("E-Invoicing offered", _.report.offerEInvoicing),
    ("Supply-chain financing offered", _.report.offerSupplyChainFinance),
    ("Policy covers charges for remaining on supplier list", _.report.retentionChargesInPolicy),
    ("Charges have been made for remaining on supplier list", _.report.retentionChargesInPast),
    ("Payment terms", _.report.paymentTermsComment),
    ("Maximum Contract Length", _.report.maximumContractPeriod),
    ("Payment terms have changed", _.report.paymentTermsChangedComment.isDefined),
    ("Payment terms have changed: comments", _.report.paymentTermsChangedComment),
    ("Suppliers notified of changes", _.report.paymentTermsChangedNotifiedComment.isDefined),
    ("Suppliers notified of changes: comments", _.report.paymentTermsChangedNotifiedComment),
    ("Further remarks on payment terms", _.report.paymentTermsComment),
    ("Dispute resolution facilities", _.report.disputeResolution),
    ("Participates in payment codes", _.report.paymentCodes.isDefined),
    ("Payment codes", _.report.paymentCodes))
}
