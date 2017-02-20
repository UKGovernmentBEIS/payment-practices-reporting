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
import db._
import forms.report.ReportFormModel
import models.{CompaniesHouseId, ReportId}
import org.joda.time.LocalDate
import org.reactivestreams.Publisher
import play.api.db.slick.DatabaseConfigProvider
import slicks.DBBinding

import scala.concurrent.{ExecutionContext, Future}

class ReportTable @Inject()(val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends DBBinding
    with ReportRepo
    with ReportModule
    with PgDateSupportJoda {

  import api._


  def find(id: ReportId): Future[Option[Report]] = db.run {
    reportQuery.filter(_._1.id === id)
      .result.headOption.map(_.map(Report.tupled))
  }

  private val reportQuery = {
    reportHeaderTable
      .joinLeft(reportPeriodTable).on(_.id === _.reportId)
      .joinLeft(paymentTermsTable).on(_._1.id === _.reportId)
      .joinLeft(paymentHistoryTable).on(_._1._1.id === _.reportId)
      .joinLeft(otherInfoTable).on(_._1._1._1.id === _.reportId)
      .joinLeft(filingTable).on(_._1._1._1._1.id === _.reportId)
      .map {
        case (((((header, period), terms), history), other), filing) => (header, period, terms, history, other, filing)
      }
  }

  def byCompanyNumber(companiesHouseId: CompaniesHouseId): Future[Seq[Report]] = db.run {
    reportQuery.filter(_._1.companyId === companiesHouseId)
      .result.map(_.map(Report.tupled))
  }

  /**
    * Code to adjust fetchSize on Postgres driver taken from:
    * https://engineering.sequra.es/2016/02/database-streaming-on-play-with-slick-from-publisher-to-chunked-result/
    */
  def list(cutoffDate: LocalDate, maxRows: Int = 100000): Publisher[FiledReport] = {
    val disableAutocommit = SimpleDBIO(_.connection.setAutoCommit(false))
    val action = reportQuery.result.withStatementParameters(fetchSize = 10000)

    db.stream(disableAutocommit andThen action).mapResult(Report.tupled)

    ???
  }

  override def save(confirmedBy: String, companiesHouseId: CompaniesHouseId, companyName: String, report: ReportFormModel): Future[ReportId] = db.run {
    ???
  }

  private def buildPaymentHistoryRow(report: ReportFormModel, reportId: ReportId) = {
    PaymentHistoryRow(
      reportId,
      report.paymentHistory.averageDaysToPay,
      report.paymentHistory.percentPaidLaterThanAgreedTerms,
      report.paymentHistory.percentageSplit.percentWithin30Days,
      report.paymentHistory.percentageSplit.percentWithin60Days,
      report.paymentHistory.percentageSplit.percentBeyond60Days
    )
  }
}
