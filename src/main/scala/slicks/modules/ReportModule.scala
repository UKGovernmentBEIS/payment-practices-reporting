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
import dbrows._
import models.{CompaniesHouseId, ReportId}
import org.joda.time.LocalDate
import slicks.DBBinding
import utils.YesNo

trait ReportModule extends DBBinding {
  self: PgDateSupportJoda =>

  import api._
  import forms.report.ReportConstants._

  implicit def yesNoMapper: BaseColumnType[YesNo] = MappedColumnType.base[YesNo, Boolean](_.toBoolean, YesNo.fromBoolean)
  implicit def reportIdMapper: BaseColumnType[ReportId] = MappedColumnType.base[ReportId, Long](_.id, ReportId)
  implicit def companiesHouseIdMapper: BaseColumnType[CompaniesHouseId] = MappedColumnType.base[CompaniesHouseId, String](_.id, CompaniesHouseId)

  type ShortFormQuery = Query[ShortFormTable, ShortFormRow, Seq]

  val reportIdColumnName = "report_id"

  class ShortFormTable(tag: Tag) extends Table[ShortFormRow](tag, "short_form") {
    def reportId = column[ReportId](reportIdColumnName, O.Length(IdType.length), O.PrimaryKey, O.AutoInc)

    def companyName = column[String]("company_name", O.Length(255))
    def companyId = column[CompaniesHouseId]("company_id", O.Length(255))
    def filingDate = column[LocalDate]("filing_date")
    def approvedBy = column[String]("approved_by", O.Length(255))
    def confirmationEmailAddress = column[String]("confirmation_email_address", O.Length(255))
    def startDate = column[LocalDate]("start_date")
    def endDate = column[LocalDate]("end_date")
    def paymentCodes = column[Option[String]]("payment_codes", O.Length(paymentCodesCharCount))

    def * = (reportId,
      companyName,
      companyId,
      filingDate,
      approvedBy,
      confirmationEmailAddress,
      startDate,
      endDate,
      paymentCodes
    ) <> (ShortFormRow.tupled, ShortFormRow.unapply)
  }

  lazy val shortFormTable = TableQuery[ShortFormTable]

  type LongFormQuery = Query[LongFormTable, LongFormRow, Seq]

  class LongFormTable(tag: Tag) extends Table[LongFormRow](tag, "long_form") {
    def reportId = column[ReportId](reportIdColumnName, O.Length(IdType.length))
    def reportIdFK = foreignKey("longForm_report_fk", reportId, shortFormTable)(_.reportId, onDelete = ForeignKeyAction.Cascade)
    def reportIdIndex = index("longForm_report_idx", reportId, unique = true)


    def paymentTerms = column[String]("payment_terms", O.Length(paymentTermsCharCount))
    def paymentPeriod = column[Int]("payment_period")
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

    def * = (reportId,
      paymentTerms,
      paymentPeriod,
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
      percentInvoicesBeyond60Days
    ) <> (LongFormRow.tupled, LongFormRow.unapply)
  }

  lazy val longFormTable = TableQuery[LongFormTable]


  override def schema =
    super.schema ++
      shortFormTable.schema ++
      longFormTable.schema
}




