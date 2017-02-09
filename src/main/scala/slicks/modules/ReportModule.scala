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

import javax.inject.Inject

import com.github.tminglei.slickpg.PgDateSupportJoda
import com.google.inject.ImplementedBy
import com.wellfactored.slickgen.IdType
import db.{ReportId, ReportRow}
import models.CompaniesHouseId
import org.joda.time.LocalDate
import play.api.db.slick.DatabaseConfigProvider
import slicks.DBBinding

import scala.concurrent.{ExecutionContext, Future}

trait ReportModule extends DBBinding {
  self: CompanyModule with PgDateSupportJoda =>

  import api._

  implicit def ReportIdMapper: BaseColumnType[ReportId] = MappedColumnType.base[ReportId, Long](_.id, ReportId)

  type ReportQuery = Query[ReportTable, ReportRow, Seq]

  class ReportTable(tag: Tag) extends Table[ReportRow](tag, "report") {
    def id = column[ReportId]("id", O.Length(IdType.length), O.PrimaryKey, O.AutoInc)

    def companyId = column[String]("company_id", O.Length(IdType.length))

    def companyIdFK = foreignKey("report_company_fk", companyId, companyTable)(_.companiesHouseIdentifier, onDelete = ForeignKeyAction.Cascade)

    def companyIdIndex = index("report_company_idx", companyId)

    def filingDate = column[LocalDate]("filing_date")

    def averageDaysToPay = column[Int]("average_days_to_pay")

    def percentInvoicesPaidBeyondAgreedTerms = column[BigDecimal]("percent_invoices_paid_beyond_agreed_terms", O.SqlType("decimal(9, 2)"))

    def percentInvoicesWithin30Days = column[BigDecimal]("percent_invoices_within_30_days", O.SqlType("decimal(9, 2)"))

    def percentInvoicesWithin60Days = column[BigDecimal]("percent_invoices_within_60_days", O.SqlType("decimal(9, 2)"))

    def percentInvoicesBeyond60Days = column[BigDecimal]("percent_invoices_beyond_60_days", O.SqlType("decimal(9, 2)"))

    def startDate = column[LocalDate]("start_date")

    def endDate = column[LocalDate]("end_date")

    def paymentTerms = column[String]("payment_terms", O.Length(255))

    def maximumContractPeriod = column[String]("maximum_contract_period", O.Length(255))

    def paymentTermsChangedComment = column[Option[String]]("payment_terms_changed_comment", O.Length(255))

    def paymentTermsChangedNotifiedComment = column[Option[String]]("payment_terms_changed_notified_comment", O.Length(255))

    def paymentTermsComment = column[Option[String]]("payment_terms_comment", O.Length(255))

    def disputeResolution = column[String]("dispute_resolution", O.Length(255))

    def offerEInvoicing = column[Boolean]("offer_einvoicing")

    def offerSupplyChainFinance = column[Boolean]("offer_supply_chain_finance")

    def retentionChargesInPolicy = column[Boolean]("retention_charges_in_policy")

    def retentionChargesInPast = column[Boolean]("retention_charges_in_past")

    def paymentCodes = column[Option[String]]("payment_codes", O.Length(255))

    def * = (id, companyId, filingDate, averageDaysToPay, percentInvoicesPaidBeyondAgreedTerms, percentInvoicesWithin30Days, percentInvoicesWithin60Days, percentInvoicesBeyond60Days, startDate, endDate, paymentTerms, maximumContractPeriod, paymentTermsChangedComment, paymentTermsChangedNotifiedComment, paymentTermsComment, disputeResolution, offerEInvoicing, offerSupplyChainFinance, retentionChargesInPolicy, retentionChargesInPast, paymentCodes) <> (ReportRow.tupled, ReportRow.unapply)
  }

  lazy val reportTable = TableQuery[ReportTable]

  override def schema = super.schema ++ reportTable.schema
}

@ImplementedBy(classOf[ReportTable])
trait ReportRepo {
  def byCompanyNumber(companiesHouseId: CompaniesHouseId): Future[Seq[ReportRow]]
}

class ReportTable @Inject()(val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends DBBinding
    with ReportRepo
    with ReportModule
    with CompanyModule
    with PgDateSupportJoda {

  import api._

  def byCompanyNumber(companiesHouseId: CompaniesHouseId): Future[Seq[ReportRow]] = db.run {
    reportTable.filter(_.companyId === companiesHouseId.id).result
  }
}