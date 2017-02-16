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
import db.ReportRow
import models.{CompaniesHouseId, ReportId}
import org.joda.time.LocalDate
import org.reactivestreams.Publisher
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

    def percentInvoicesPaidBeyondAgreedTerms = column[Int]("percent_invoices_paid_beyond_agreed_terms")

    def percentInvoicesWithin30Days = column[Int]("percent_invoices_within_30_days")

    def percentInvoicesWithin60Days = column[Int]("percent_invoices_within_60_days")

    def percentInvoicesBeyond60Days = column[Int]("percent_invoices_beyond_60_days")

    def startDate = column[LocalDate]("start_date")

    def endDate = column[LocalDate]("end_date")

    def paymentTerms = column[String]("payment_terms", O.Length(255))

    def paymentPeriod = column[Int]("payment_period")

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

    def * = (id, companyId, filingDate, averageDaysToPay, percentInvoicesPaidBeyondAgreedTerms, percentInvoicesWithin30Days, percentInvoicesWithin60Days, percentInvoicesBeyond60Days, startDate, endDate, paymentTerms, paymentPeriod, maximumContractPeriod, paymentTermsChangedComment, paymentTermsChangedNotifiedComment, paymentTermsComment, disputeResolution, offerEInvoicing, offerSupplyChainFinance, retentionChargesInPolicy, retentionChargesInPast, paymentCodes) <> (ReportRow.tupled, ReportRow.unapply)
  }

  lazy val reportTable = TableQuery[ReportTable]

  override def schema = super.schema ++ reportTable.schema
}

case class CompanyReport(name: String, report: ReportRow)

@ImplementedBy(classOf[ReportTable])
trait ReportRepo {
  def find(id: ReportId): Future[Option[ReportRow]]

  def byCompanyNumber(companiesHouseId: CompaniesHouseId): Future[Seq[ReportRow]]

  def list(cutoffDate: LocalDate, maxRows: Int = 100000): Publisher[CompanyReport]
}

class ReportTable @Inject()(val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends DBBinding
    with ReportRepo
    with ReportModule
    with CompanyModule
    with PgDateSupportJoda {

  import api._

  def find(id: ReportId): Future[Option[ReportRow]] = db.run(reportTable.filter(_.id === id).result.headOption)

  def byCompanyNumber(companiesHouseId: CompaniesHouseId): Future[Seq[ReportRow]] = db.run {
    reportTable.filter(_.companyId === companiesHouseId.id).result
  }

  /**
    * Code to adjust fetchSize on Postgres driver taken from:
    * https://engineering.sequra.es/2016/02/database-streaming-on-play-with-slick-from-publisher-to-chunked-result/
    */
  def list(cutoffDate: LocalDate, maxRows: Int = 100000): Publisher[CompanyReport] = {
    val disableAutocommit = SimpleDBIO(_.connection.setAutoCommit(false))
    val query = for {
      report <- reportTable.filter(_.filingDate >= cutoffDate).take(maxRows)
      company <- companyTable.filter(_.companiesHouseIdentifier === report.companyId).map(_.name)
    } yield (company, report)

    val action = query.result.withStatementParameters(fetchSize = 10000)

    db.stream(disableAutocommit andThen action).mapResult(p => CompanyReport(p._1, p._2))
  }
}