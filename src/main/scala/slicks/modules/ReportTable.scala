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
import db.{CompanyRow, PaymentHistoryRow, ReportRow}
import forms.report.ReportFormModel
import models.{CompaniesHouseId, PaymentHistoryId, ReportId}
import org.joda.time.LocalDate
import org.reactivestreams.Publisher
import play.api.db.slick.DatabaseConfigProvider
import slicks.DBBinding

import scala.concurrent.{ExecutionContext, Future}

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

  override def save(confirmedBy: String, companiesHouseId: CompaniesHouseId, companyName: String, report: ReportFormModel): Future[ReportId] = db.run {
    companyTable.filter(_.companiesHouseIdentifier === companiesHouseId.id).result.headOption.flatMap {
      case None => companyTable.returning(companyTable.map(_.companiesHouseIdentifier)) += CompanyRow(companiesHouseId.id, companyName)
      case Some(c) => DBIO.successful(c.companiesHouseIdentifier)
    }.flatMap { cId =>
      reportTable.returning(reportTable.map(_.id)) += buildReportRow(confirmedBy, cId, report)
    }.flatMap { (reportId: ReportId) =>
      (paymentHistoryTable += buildPaymentHistoryRow(report, reportId)).map(_ => reportId)
    }.transactionally
  }

  private def buildPaymentHistoryRow(report: ReportFormModel, reportId: ReportId) = {
    PaymentHistoryRow(
      PaymentHistoryId(0),
      reportId,
      report.paymentHistory.averageDaysToPay,
      report.paymentHistory.percentPaidLaterThanAgreedTerms,
      report.paymentHistory.percentageSplit.percentWithin30Days,
      report.paymentHistory.percentageSplit.percentWithin60Days,
      report.paymentHistory.percentageSplit.percentBeyond60Days
    )
  }

  private def buildReportRow(confirmedBy: String, cId: String, report: ReportFormModel) = {
    ReportRow(
      ReportId(0),
      cId,
      report.filingDate,
      report.reportDates.startDate,
      report.reportDates.endDate,
      report.paymentTerms.terms,
      report.paymentTerms.paymentPeriod,
      report.paymentTerms.maximumContractPeriod,
      report.paymentTerms.maximumContractPeriodComment,
      report.paymentTerms.paymentTermsComment,
      report.paymentTerms.paymentTermsChangedNotified.text,
      report.paymentTerms.paymentTermsComment,
      report.disputeResolution,
      report.offerEInvoicing,
      report.offerSupplyChainFinancing,
      report.retentionChargesInPolicy,
      report.retentionChargesInPast,
      report.hasPaymentCodes.text,
      confirmedBy
    )
  }
}
