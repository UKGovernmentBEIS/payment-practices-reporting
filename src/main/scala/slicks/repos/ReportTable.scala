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

import dbrows.{CommentRow, ConfirmationPendingRow}
import forms.report.{LongFormModel, ReportingPeriodFormModel, ShortFormModel}
import models.{CommentId, CompaniesHouseId, ReportId}
import org.joda.time.{LocalDate, LocalDateTime}
import org.reactivestreams.Publisher
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import services._
import slick.jdbc.JdbcProfile
import slicks.helpers.RowBuilders
import slicks.modules.{ConfirmationModule, CoreModule, ReportModule}

import scala.concurrent.{ExecutionContext, Future}

class ReportTable @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends CoreModule
    with ReportService
    with ReportModule
    with ConfirmationModule
    with ReportQueries
    with RowBuilders
    with HasDatabaseConfig[JdbcProfile] {

  override lazy val dbConfig = dbConfigProvider.get[JdbcProfile]

  import profile.api._

  //noinspection TypeAnnotation
  def activeReportByIdQ(reportId: Rep[ReportId]) = activeReportQuery.filter(_._1.id === reportId)

  val activeReportByIdC = Compiled(activeReportByIdQ _)

  def find(id: ReportId): Future[Option[Report]] = db.run {
    activeReportByIdC(id).result.headOption.map(_.map(Report.apply))
  }

  //noinspection TypeAnnotation
  def archivedReportByIdQ(reportId: Rep[ReportId]) = archivedReportQuery.filter(_._1.id === reportId)

  val archivedReportByIdC = Compiled(archivedReportByIdQ _)

  def findArchived(id: ReportId): Future[Option[Report]] = db.run {
    archivedReportByIdC(id).result.headOption.map(_.map(Report.apply))
  }


  //noinspection TypeAnnotation
  def reportByCoNoQ(cono: Rep[CompaniesHouseId]) = activeReportQuery.filter(_._1.companyId === cono)

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
    val action = activeReportQueryC.result.withStatementParameters(fetchSize = 10000)

    db.stream(disableAutocommit andThen action).mapResult(Report.apply)
  }

  override def createLongReport(
    companyDetail: CompanyDetail,
    reportingPeriod: ReportingPeriodFormModel,
    longForm: LongFormModel,
    confirmedBy: String,
    confirmationEmailAddress: String,
    reportUrl: (ReportId) => String
  ): Future[ReportId] = db.run {
    val reportRow = buildReport(companyDetail, confirmedBy, reportingPeriod, longForm.otherInformation.paymentCodes, confirmationEmailAddress)

    {
      for {
        reportId <- reportTable.returning(reportTable.map(_.id)) += reportRow
        _ <- contractDetailsTable += buildContractDetails(reportId, longForm)
        _ <- confirmationPendingTable += ConfirmationPendingRow(reportId, confirmationEmailAddress, reportUrl(reportId), 0, None, None, None)
      } yield reportId
    }.transactionally
  }

  override def createShortReport(
    companyDetail: CompanyDetail,
    reportingPeriod: ReportingPeriodFormModel,
    shortFormModel: ShortFormModel,
    confirmedBy: String,
    confirmationEmailAddress: String,
    reportUrl: (ReportId) => String
  ): Future[ReportId] = db.run {
    val reportRow = buildReport(companyDetail, confirmedBy, reportingPeriod, shortFormModel.paymentCodes, confirmationEmailAddress)

    {
      for {
        reportId <- reportTable.returning(reportTable.map(_.id)) += reportRow
        _ <- confirmationPendingTable += ConfirmationPendingRow(reportId, confirmationEmailAddress, reportUrl(reportId), 0, None, None, None)
      } yield reportId
    }.transactionally
  }


  override def archive(id: ReportId, timestamp: LocalDateTime, comment: String): Future[ArchiveResult] = db.run {
    reportTable.filter(_.id === id).result.headOption.flatMap {
      case None                                        => DBIO.successful(ArchiveResult.NotFound)
      case Some(report) if report.archivedOn.isDefined => DBIO.successful(ArchiveResult.AlreadyArchived)
      case Some(report)                                => for {
        _ <- reportTable.filter(_.id === report.id).map(_.archivedOn).update(Some(timestamp))
        _ <- commentTable += CommentRow(CommentId(0), id, comment, timestamp)
      } yield ArchiveResult.Archived
    }.transactionally
  }

  override def unarchive(id: ReportId, timestamp: LocalDateTime, comment: String): Future[UnarchiveResult] = db.run {
    reportTable.filter(_.id === id).result.headOption.flatMap {
      case None                                      => DBIO.successful(UnarchiveResult.NotFound)
      case Some(report) if report.archivedOn.isEmpty => DBIO.successful(UnarchiveResult.NotArchived)
      case Some(report)                              => for {
        _ <- reportTable.filter(_.id === report.id).map(_.archivedOn).update(Some(timestamp))
        _ <- commentTable += CommentRow(CommentId(0), id, comment, timestamp)
      } yield UnarchiveResult.Unarchived
    }.transactionally
  }
}
