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

package slicks.repos

import javax.inject.Inject

import forms.report.{LongFormModel, ShortFormModel, ReportReviewModel, ReportingPeriodFormModel}
import models.{CompaniesHouseId, ReportId}
import org.joda.time.LocalDate
import org.reactivestreams.Publisher
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import services.{CompanyDetail, Report, ReportService}
import slick.jdbc.JdbcProfile
import slicks.helpers.RowBuilders
import slicks.modules.{ConfirmationModule, CoreModule, ReportModule}

import scala.concurrent.{ExecutionContext, Future}

class ReportTable @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends CoreModule
    with ReportService
    with ReportModule
    with ReportQueries
    with RowBuilders
    with HasDatabaseConfig[JdbcProfile] {

  override lazy val dbConfig = dbConfigProvider.get[JdbcProfile]

  import profile.api._

  def reportByIdQ(reportId: Rep[ReportId]) = reportQuery.filter(_._1.id === reportId)

  val reportByIdC = Compiled(reportByIdQ _)

  def find(id: ReportId): Future[Option[Report]] = db.run {
    reportByIdC(id).result.headOption.map(_.map(Report.apply))
  }

  def reportByCoNoQ(cono: Rep[CompaniesHouseId]) = reportQuery.filter(_._1.companyId === cono)

  val reportByCoNoC = Compiled(reportByCoNoQ _)

  def byCompanyNumber(companiesHouseId: CompaniesHouseId): Future[Seq[Report]] = db.run {
    reportByCoNoC(companiesHouseId).result.map(_.map(Report.apply))
  }

  /**
    * Code to adjust fetchSize on Postgres driver taken from:
    * https://engineering.sequra.es/2016/02/database-streaming-on-play-with-slick-from-publisher-to-chunked-result/
    */
  def list(cutoffDate: LocalDate): Publisher[Report] = {
    val disableAutocommit = SimpleDBIO(_.connection.setAutoCommit(false))
    val action = reportQueryC.result.withStatementParameters(fetchSize = 10000)

    db.stream(disableAutocommit andThen action).mapResult(Report.apply)
  }

  override def create(
                       companyDetail: CompanyDetail,
                       reportingPeriod: ReportingPeriodFormModel,
                       longForm: LongFormModel,
                       review: ReportReviewModel,
                       confirmationEmailAddress: String,
                       reportUrl: (ReportId) => String): Future[ReportId] = db.run {
    val reportRow = buildReport(companyDetail, review, reportingPeriod, longForm.paymentCodes, confirmationEmailAddress)

    {
      for {
        reportId <- reportTable.returning(reportTable.map(_.id)) += reportRow
        _ <- contractDetailsTable += buildContractDetails(reportId, longForm)
      } yield reportId
    }.transactionally
  }
  override def create(
                       companyDetail: CompanyDetail,
                       reportingPeriod: ReportingPeriodFormModel,
                       shortFormModel: ShortFormModel,
                       review: ReportReviewModel,
                       confirmationEmailAddress: String,
                       reportUrl: (ReportId) => String): Future[ReportId] = db.run {
    val reportRow = buildReport(companyDetail, review, reportingPeriod, shortFormModel.paymentCodes, confirmationEmailAddress)
    reportTable.returning(reportTable.map(_.id)) += reportRow
  }
}
