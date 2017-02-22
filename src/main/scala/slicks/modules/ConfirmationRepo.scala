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

import com.google.inject.ImplementedBy
import db.ConfirmationPendingRow
import models.ReportId
import org.joda.time.LocalDateTime
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[ConfirmationTable])
trait ConfirmationRepo {
  def findUnconfirmedAndLock(): Future[Option[ConfirmationPendingRow]]

  def sentAt(reportId: ReportId, when: LocalDateTime): Future[Unit]
}

class ConfirmationTable @Inject()(val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends ConfirmationRepo
    with ConfirmationModule
    with ReportModule {

  import api._

  override def findUnconfirmedAndLock(): Future[Option[ConfirmationPendingRow]] = db.run {
    val lockTimeout = LocalDateTime.now().minusSeconds(30)

    val row = confirmationPendingTable
      .filter(c => c.lockedAt.isEmpty || c.lockedAt < lockTimeout)
      .result
      .headOption

    row.flatMap {
      case Some(r) =>
        confirmationPendingTable
          .filter(_.reportId === r.reportId)
          .map(_.lockedAt)
          .update(Some(LocalDateTime.now())).map(_ => Some(r))

      case None => DBIO.successful(None)
    }.transactionally
  }


  override def sentAt(reportId: ReportId, when: LocalDateTime): Future[Unit] = db.run {
    ???
  }
}