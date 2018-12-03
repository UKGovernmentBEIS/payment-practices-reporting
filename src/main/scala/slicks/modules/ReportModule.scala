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

package slicks.modules


import dbrows._
import models.{CommentId, CompaniesHouseId, ReportId}
import org.joda.time.{LocalDate, LocalDateTime}
import utils.YesNo

trait ReportModule {
  self: CoreModule =>

  import forms.report.ReportConstants._
  import profile.api._

  implicit def yesNoMapper: BaseColumnType[YesNo] = MappedColumnType.base[YesNo, Boolean](_.toBoolean, YesNo.fromBoolean)
  implicit def reportIdMapper: BaseColumnType[ReportId] = MappedColumnType.base[ReportId, Long](_.id, ReportId)
  implicit def commentIdMapper: BaseColumnType[CommentId] = MappedColumnType.base[CommentId, Long](_.id, CommentId)
  implicit def companiesHouseIdMapper: BaseColumnType[CompaniesHouseId] = MappedColumnType.base[CompaniesHouseId, String](_.id, CompaniesHouseId)

  type ReportQuery = Query[ReportTable, ReportRow, Seq]

  val reportIdColumnName = "report_id"

  class ReportTable(tag: Tag) extends Table[ReportRow](tag, "report") {
    def id = column[ReportId](reportIdColumnName, O.Length(IdType.length), O.PrimaryKey, O.AutoInc)

    def companyName = column[String]("company_name", O.Length(255))
    def companyId = column[CompaniesHouseId]("company_id", O.Length(255))
    def filingDate = column[LocalDate]("filing_date")
    def approvedBy = column[String]("approved_by", O.Length(255))
    def confirmationEmailAddress = column[String]("confirmation_email_address", O.Length(255))
    def startDate = column[LocalDate]("start_date")
    def endDate = column[LocalDate]("end_date")
    def paymentCodes = column[Option[String]]("payment_codes", O.Length(paymentCodesCharCount))
    def archivedOn = column[Option[LocalDateTime]]("archived_on")

    def * = (id,
      companyName,
      companyId,
      filingDate,
      approvedBy,
      confirmationEmailAddress,
      startDate,
      endDate,
      paymentCodes,
      archivedOn
    ) <> (ReportRow.tupled, ReportRow.unapply)
  }

  lazy val reportTable = TableQuery[ReportTable]

  type ContractDetailsQuery = Query[ContractDetailsTable, ContractDetailsRow, Seq]

  class ContractDetailsTable(tag: Tag) extends Table[ContractDetailsRow](tag, "contract_details") {
    def reportId = column[ReportId](reportIdColumnName, O.Length(IdType.length), O.Unique)
    def reportIdFK = foreignKey("long_form_report_fk", reportId, reportTable)(_.id, onDelete = ForeignKeyAction.Cascade)
    def reportIdIndex = index("long_form_report_idx", reportId, unique = true)

    def paymentTerms = column[String]("payment_terms", O.Length(paymentTermsCharCount))
    def shortestPaymentPeriod = column[Int]("shortest_payment_period")
    def longestPaymentPeriod = column[Option[Int]]("longest_payment_period")
    def maximumContractPeriod = column[Int]("maximum_contract_period")
    def maximumContractPeriodComment = column[Option[String]]("maximum_contract_period_comment", O.Length(maxContractPeriodCommentCharCount))
    def paymentTermsChangedComment = column[Option[String]]("payment_terms_changed_comment", O.Length(paymentTermsChangedCharCount))
    def paymentTermsChangedNotifiedComment = column[Option[String]]("payment_terms_changed_notified_comment", O.Length(paymentTermsNotifiedCharCount))
    def paymentTermsComment = column[Option[String]]("payment_terms_comment", O.Length(paymentTermsCommentCharCount))
    def disputeResolution = column[String]("dispute_resolution", O.Length(disputeResolutionCharCount))

    def offerEInvoicing = column[YesNo]("offer_einvoicing")
    def offerSupplyChainFinance = column[YesNo]("offer_supply_chain_finance")
    def retentionChargesInPolicy = column[YesNo]("retention_charges_in_policy")
    def retentionChargesInPast = column[YesNo]("retention_charges_in_past")

    def averageDaysToPay = column[Int]("average_days_to_pay")
    def percentPaidLaterThanAgreedTerms = column[Int]("percent_paid_later_than_agreed_terms")
    def percentInvoicesWithin30Days = column[Int]("percent_invoices_within30days")
    def percentInvoicesWithin60Days = column[Int]("percent_invoices_within60days")
    def percentInvoicesBeyond60Days = column[Int]("percent_invoices_beyond60days")

    def didMakePayment = column[Option[YesNo]]("did_make_payment")

    def * = (reportId,
      paymentTerms,
      shortestPaymentPeriod,
      longestPaymentPeriod,
      maximumContractPeriod,
      maximumContractPeriodComment,
      paymentTermsChangedComment,
      paymentTermsChangedNotifiedComment,
      paymentTermsComment,
      disputeResolution,
      offerEInvoicing,
      offerSupplyChainFinance,
      retentionChargesInPolicy,
      retentionChargesInPast,
      averageDaysToPay,
      percentPaidLaterThanAgreedTerms,
      percentInvoicesWithin30Days,
      percentInvoicesWithin60Days,
      percentInvoicesBeyond60Days,
      didMakePayment
    ) <> (ContractDetailsRow.tupled, ContractDetailsRow.unapply)
  }

  lazy val contractDetailsTable = TableQuery[ContractDetailsTable]

  type CommentQuery = Query[CommentTable, CommentRow, Seq]

  class CommentTable(tag: Tag) extends Table[CommentRow](tag, "comment") {
    def id = column[CommentId]("id", O.Length(IdType.length), O.PrimaryKey, O.AutoInc)

    def reportId = column[ReportId](reportIdColumnName, O.Length(IdType.length), O.Unique)
    def reportIdFK = foreignKey("comment_report_fk", reportId, reportTable)(_.id, onDelete = ForeignKeyAction.Cascade)

    def comment = column[String]("comment")
    def timestamp = column[LocalDateTime]("timestamp")

    def * = (id, reportId, comment, timestamp) <> (CommentRow.tupled, CommentRow.unapply)
  }

  lazy val commentTable = TableQuery[CommentTable]

}




