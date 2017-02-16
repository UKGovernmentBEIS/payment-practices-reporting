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
import db.{PaymentHistoryRow, ReportRow}
import models.{CompaniesHouseId, PaymentHistoryId, ReportId}
import org.joda.time.LocalDate
import org.reactivestreams.Publisher
import play.api.db.slick.DatabaseConfigProvider
import slicks.DBBinding

import scala.concurrent.{ExecutionContext, Future}

trait ReportModule extends DBBinding {
  self: CompanyModule with PgDateSupportJoda =>

  val wordLength = 7
  val longTerms = wordLength * 5000
  val shortComment = wordLength * 500
  val longComment = wordLength * 2000

  import api._

  implicit def PaymentHistoryIdMapper: BaseColumnType[PaymentHistoryId] = MappedColumnType.base[PaymentHistoryId, Long](_.id, PaymentHistoryId)

  implicit def ReportIdMapper: BaseColumnType[ReportId] = MappedColumnType.base[ReportId, Long](_.id, ReportId)


  type ReportQuery = Query[ReportTable, ReportRow, Seq]

  class ReportTable(tag: Tag) extends Table[ReportRow](tag, "report") {
    def id = column[ReportId]("id", O.Length(IdType.length), O.PrimaryKey, O.AutoInc)

    def companyId = column[String]("company_id", O.Length(IdType.length))

    def companyIdFK = foreignKey("report_company_fk", companyId, companyTable)(_.companiesHouseIdentifier, onDelete = ForeignKeyAction.Cascade)

    def companyIdIndex = index("report_company_idx", companyId)

    def filingDate = column[LocalDate]("filing_date")

    def startDate = column[LocalDate]("start_date")

    def endDate = column[LocalDate]("end_date")

    def paymentTerms = column[String]("payment_terms", O.Length(longTerms))

    def paymentPeriod = column[Int]("payment_period")

    def maximumContractPeriod = column[Int]("maximum_contract_period")

    def maximumContractPeriodComment = column[Option[String]]("maximum_contract_period_comment", O.Length(shortComment))

    def paymentTermsChangedComment = column[Option[String]]("payment_terms_changed_comment", O.Length(shortComment))

    def paymentTermsChangedNotifiedComment = column[Option[String]]("payment_terms_changed_notified_comment", O.Length(shortComment))

    def paymentTermsComment = column[Option[String]]("payment_terms_comment", O.Length(longComment))

    def disputeResolution = column[String]("dispute_resolution", O.Length(longTerms))

    def offerEInvoicing = column[Boolean]("offer_einvoicing")

    def offerSupplyChainFinance = column[Boolean]("offer_supply_chain_finance")

    def retentionChargesInPolicy = column[Boolean]("retention_charges_in_policy")

    def retentionChargesInPast = column[Boolean]("retention_charges_in_past")

    def paymentCodes = column[Option[String]]("payment_codes", O.Length(255))

    def * = (
      id,
      companyId,
      filingDate,
      startDate,
      endDate,
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
      paymentCodes
    ) <> (ReportRow.tupled, ReportRow.unapply)
  }

  lazy val reportTable = TableQuery[ReportTable]

  type PaymentHistoryQuery = Query[PaymentHistoryTable, PaymentHistoryRow, Seq]

  class PaymentHistoryTable(tag: Tag) extends Table[PaymentHistoryRow](tag, "payment_history") {
    def id = column[PaymentHistoryId]("id", O.Length(IdType.length), O.PrimaryKey)

    def reportId = column[ReportId]("report_id", O.Length(IdType.length))

    def reportIdFK = foreignKey("paymenthistory_report_fk", reportId, reportTable)(_.id, onDelete = ForeignKeyAction.Cascade)

    def reportIdIndex = index("paymenthistory_report_idx", reportId)

    def averageDaysToPay = column[Int]("average_days_to_pay")

    def percentPaidLaterThanAgreedTerms = column[Int]("percent_paid_later_than_agreed_terms")

    def percentInvoicesWithin30Days = column[Int]("percent_invoices_within30days")

    def percentInvoicesWithin60Days = column[Int]("percent_invoices_within60days")

    def percentInvoicesBeyond60Days = column[Int]("percent_invoices_beyond60days")

    def * = (id, reportId, averageDaysToPay, percentPaidLaterThanAgreedTerms, percentInvoicesWithin30Days, percentInvoicesWithin60Days, percentInvoicesBeyond60Days) <> (PaymentHistoryRow.tupled, PaymentHistoryRow.unapply)
  }

  lazy val paymentHistoryTable = TableQuery[PaymentHistoryTable]

  override def schema = super.schema ++ reportTable.schema ++ paymentHistoryTable.schema
}

case class CompanyReport(name: String, report: ReportRow, paymentHistory: PaymentHistoryRow)

@ImplementedBy(classOf[ReportTable])
trait ReportRepo {
  def find(id: ReportId): Future[Option[ReportRow]]

  def reportFor(id: ReportId): Future[Option[CompanyReport]]

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

  def reportFor(id: ReportId): Future[Option[CompanyReport]] = db.run {
    val query = for {
      report <- reportTable.filter(_.id === id)
      paymentHistory <- paymentHistoryTable.filter(_.reportId === report.id)
      company <- companyTable.filter(_.companiesHouseIdentifier === report.companyId).map(_.name)
    } yield (company, report, paymentHistory)

    query.result.map(_.map(CompanyReport.tupled).headOption)
  }

  /**
    * Code to adjust fetchSize on Postgres driver taken from:
    * https://engineering.sequra.es/2016/02/database-streaming-on-play-with-slick-from-publisher-to-chunked-result/
    */
  def list(cutoffDate: LocalDate, maxRows: Int = 100000): Publisher[CompanyReport] = {
    val disableAutocommit = SimpleDBIO(_.connection.setAutoCommit(false))

    val query = for {
      report <- reportTable.filter(_.filingDate >= cutoffDate).take(maxRows)
      paymentHistory <- paymentHistoryTable.filter(_.reportId === report.id)
      company <- companyTable.filter(_.companiesHouseIdentifier === report.companyId).map(_.name)
    } yield (company, report, paymentHistory)

    val action = query.result.withStatementParameters(fetchSize = 10000)

    db.stream(disableAutocommit andThen action).mapResult(CompanyReport.tupled)
  }
}