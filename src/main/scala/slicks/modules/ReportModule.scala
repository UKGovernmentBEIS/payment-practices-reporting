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

import com.github.tminglei.slickpg.PgDateSupportJoda
import com.wellfactored.slickgen.IdType
import db._
import models.{CompaniesHouseId, ReportId}
import org.joda.time.LocalDate
import slicks.DBBinding
import utils.YesNo

trait ReportModule extends DBBinding {
  self: PgDateSupportJoda =>

  val wordLength = 7
  val longTerms = wordLength * 5000
  val shortComment = wordLength * 500
  val longComment = wordLength * 2000

  import api._

  implicit def YesNoMapper: BaseColumnType[YesNo] = MappedColumnType.base[YesNo, Boolean](_.toBoolean, YesNo.fromBoolean)

  implicit def ReportIdMapper: BaseColumnType[ReportId] = MappedColumnType.base[ReportId, Long](_.id, ReportId)

  implicit def CompaniesHouseIdMapper: BaseColumnType[CompaniesHouseId] = MappedColumnType.base[CompaniesHouseId, String](_.id, CompaniesHouseId)


  type FilingQuery = Query[FilingTable, FilingRow, Seq]

  class FilingTable(tag: Tag) extends Table[FilingRow](tag, "filing") {
    def reportId = column[ReportId]("report_id", O.Length(IdType.length))

    def reportIdFK = foreignKey("filing_report_fk", reportId, reportHeaderTable)(_.id, onDelete = ForeignKeyAction.Cascade)

    def reportIdIndex = index("filing_report_idx", reportId, unique = true)

    def filingDate = column[LocalDate]("filing_date")

    def approvedBy = column[String]("approved_by", O.Length(255))

    def confirmationEmailAddress = column[String]("confirmation_email_address", O.Length(255))

    def * = (reportId, filingDate, approvedBy, confirmationEmailAddress) <> (FilingRow.tupled, FilingRow.unapply)
  }

  lazy val filingTable = TableQuery[FilingTable]

  type OtherInfoQuery = Query[OtherInfoTable, OtherInfoRow, Seq]

  class OtherInfoTable(tag: Tag) extends Table[OtherInfoRow](tag, "other_info") {
    def reportId = column[ReportId]("report_id", O.Length(IdType.length))

    def reportIdFK = foreignKey("otherinfo_report_fk", reportId, reportHeaderTable)(_.id, onDelete = ForeignKeyAction.Cascade)

    def reportIdIndex = index("otherinfo_report_idx", reportId, unique = true)

    def offerEInvoicing = column[YesNo]("offer_einvoicing")

    def offerSupplyChainFinance = column[YesNo]("offer_supply_chain_finance")

    def retentionChargesInPolicy = column[YesNo]("retention_charges_in_policy")

    def retentionChargesInPast = column[YesNo]("retention_charges_in_past")

    def paymentCodes = column[Option[String]]("payment_codes", O.Length(255))

    def * = (reportId, offerEInvoicing, offerSupplyChainFinance, retentionChargesInPolicy, retentionChargesInPast, paymentCodes) <> (OtherInfoRow.tupled, OtherInfoRow.unapply)
  }

  lazy val otherInfoTable = TableQuery[OtherInfoTable]

  type PaymentHistoryQuery = Query[PaymentHistoryTable, PaymentHistoryRow, Seq]

  class PaymentHistoryTable(tag: Tag) extends Table[PaymentHistoryRow](tag, "payment_history") {
    def reportId = column[ReportId]("report_id", O.Length(IdType.length))

    def reportIdFK = foreignKey("paymenthistory_report_fk", reportId, reportHeaderTable)(_.id, onDelete = ForeignKeyAction.Cascade)

    def reportIdIndex = index("paymenthistory_report_idx", reportId, unique = true)

    def averageDaysToPay = column[Int]("average_days_to_pay")

    def percentPaidLaterThanAgreedTerms = column[Int]("percent_paid_later_than_agreed_terms")

    def percentInvoicesWithin30Days = column[Int]("percent_invoices_within30days")

    def percentInvoicesWithin60Days = column[Int]("percent_invoices_within60days")

    def percentInvoicesBeyond60Days = column[Int]("percent_invoices_beyond60days")

    def * = (reportId, averageDaysToPay, percentPaidLaterThanAgreedTerms, percentInvoicesWithin30Days, percentInvoicesWithin60Days, percentInvoicesBeyond60Days) <> (PaymentHistoryRow.tupled, PaymentHistoryRow.unapply)
  }

  lazy val paymentHistoryTable = TableQuery[PaymentHistoryTable]

  type PaymentTermsQuery = Query[PaymentTermsTable, PaymentTermsRow, Seq]

  class PaymentTermsTable(tag: Tag) extends Table[PaymentTermsRow](tag, "payment_terms") {
    def reportId = column[ReportId]("report_id", O.Length(IdType.length))

    def reportIdFK = foreignKey("paymentterms_report_fk", reportId, reportHeaderTable)(_.id, onDelete = ForeignKeyAction.Cascade)

    def reportIdIndex = index("paymentterms_report_idx", reportId, unique = true)

    def paymentTerms = column[String]("payment_terms", O.Length(longTerms))

    def paymentPeriod = column[Int]("payment_period")

    def maximumContractPeriod = column[Int]("maximum_contract_period")

    def maximumContractPeriodComment = column[Option[String]]("maximum_contract_period_comment", O.Length(shortComment))

    def paymentTermsChangedComment = column[Option[String]]("payment_terms_changed_comment", O.Length(shortComment))

    def paymentTermsChangedNotifiedComment = column[Option[String]]("payment_terms_changed_notified_comment", O.Length(shortComment))

    def paymentTermsComment = column[Option[String]]("payment_terms_comment", O.Length(shortComment))

    def disputeResolution = column[String]("dispute_resolution", O.Length(longComment))

    def * = (reportId, paymentTerms, paymentPeriod, maximumContractPeriod, maximumContractPeriodComment, paymentTermsChangedComment, paymentTermsChangedNotifiedComment, paymentTermsComment, disputeResolution) <> (PaymentTermsRow.tupled, PaymentTermsRow.unapply)
  }

  lazy val paymentTermsTable = TableQuery[PaymentTermsTable]

  type ReportPeriodQuery = Query[ReportPeriodTable, ReportPeriodRow, Seq]

  class ReportPeriodTable(tag: Tag) extends Table[ReportPeriodRow](tag, "report_period") {
    def reportId = column[ReportId]("report_id", O.Length(IdType.length))

    def reportIdFK = foreignKey("reportperiod_report_fk", reportId, reportHeaderTable)(_.id, onDelete = ForeignKeyAction.Cascade)

    def reportIdIndex = index("reportperiod_report_idx", reportId, unique = true)

    def startDate = column[LocalDate]("start_date")

    def endDate = column[LocalDate]("end_date")

    def * = (reportId, startDate, endDate) <> (ReportPeriodRow.tupled, ReportPeriodRow.unapply)
  }

  lazy val reportPeriodTable = TableQuery[ReportPeriodTable]

  type ReportHeaderQuery = Query[ReportHeaderTable, ReportHeaderRow, Seq]

  class ReportHeaderTable(tag: Tag) extends Table[ReportHeaderRow](tag, "report_header") {
    def id = column[ReportId]("id", O.Length(IdType.length), O.PrimaryKey, O.AutoInc)

    def companyName = column[String]("company_name", O.Length(255))

    def companyId = column[CompaniesHouseId]("company_id", O.Length(255))

    def createdAt = column[LocalDate]("created_at")

    def updatedAt = column[LocalDate]("updated_at")

    def * = (id, companyName, companyId, createdAt, updatedAt) <> (ReportHeaderRow.tupled, ReportHeaderRow.unapply)
  }

  lazy val reportHeaderTable = TableQuery[ReportHeaderTable]

  override def schema =
    super.schema ++
      reportHeaderTable.schema ++
      reportPeriodTable.schema ++
      paymentTermsTable.schema ++
      paymentHistoryTable.schema ++
      otherInfoTable.schema ++
      filingTable.schema
}




