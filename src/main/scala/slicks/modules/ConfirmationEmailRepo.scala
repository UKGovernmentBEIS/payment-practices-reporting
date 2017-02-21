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

import db.ConfirmationEmailRow
import models.ReportId
import org.joda.time.LocalDateTime
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

trait ConfirmationEmailRepo {
  def findUnsentAndLock(): Future[Option[ConfirmationEmailRow]]

  def updateEmailAddress(reportId: ReportId, email: String): Future[Unit]

  def unlock(reportId: ReportId): Future[Unit]
}

class ConfirmationEmailTable @Inject()(val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends ConfirmationEmailRepo
    with ConfirmationEmailModule
    with ReportModule {

  import api._

  override def findUnsentAndLock(): Future[Option[ConfirmationEmailRow]] = db.run {
    val lockTimeout = LocalDateTime.now().minusSeconds(30)

    val row = confirmationEmailTable
      .filter(c => c.sentAt.isEmpty && (c.lockedAt.isEmpty || c.lockedAt < lockTimeout))
      .result
      .headOption

    row.flatMap {
      case Some(r) =>
        confirmationEmailTable
          .filter(_.reportId === r.reportId)
          .map(_.lockedAt)
          .update(Some(LocalDateTime.now())).map(_ => Some(r))
      case None => findUnattemptedAndLock()
    }.transactionally
  }

  private def findUnattemptedAndLock(): DBIO[Option[ConfirmationEmailRow]] = {
    val q = reportHeaderTable.joinLeft(confirmationEmailTable).filter(_._2.isEmpty).map(_._1)

    q.result.headOption.map {
      case Some(rh) =>
        val row = ConfirmationEmailRow(rh.id, None, None, Some(LocalDateTime.now))
        confirmationEmailTable += row
        Some(row)
      case None => None
    }
  }

  def unlock(reportId: ReportId): Future[Unit] = db.run {
    confirmationEmailTable.filter(_.reportId === reportId).map(_.lockedAt).update(None).map(_ => ())
  }

  def updateEmailAddress(reportId: ReportId, emailAddress: String): Future[Unit] = db.run {
    confirmationEmailTable.filter(_.reportId === reportId).map(_.emailAddress).update(Some(emailAddress)).map(_ => ())
  }
}