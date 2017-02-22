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
import db.{ConfirmationFailedRow, ConfirmationPendingRow, ConfirmationSentRow}
import models.ReportId
import org.joda.time.LocalDateTime
import slicks.DBBinding

trait ConfirmationModule extends DBBinding {
  self: ReportModule with PgDateSupportJoda =>

  import api._

  type ConfirmationFailedQuery = Query[ConfirmationFailedTable, ConfirmationFailedRow, Seq]

  class ConfirmationFailedTable(tag: Tag) extends Table[ConfirmationFailedRow](tag, "confirmation_failed") {
    def reportId = column[ReportId]("report_id", O.Length(IdType.length))

    def reportIdFK = foreignKey("confirmationfailed_report_fk", reportId, reportHeaderTable)(_.id, onDelete = ForeignKeyAction.Cascade)

    def reportIdIndex = index("confirmationfailed_report_idx", reportId, unique = true)

    def emailAddress = column[String]("email_address", O.Length(255))

    def errorStatus = column[Int]("error_status")

    def errorText = column[String]("error_text", O.Length(2048))

    def failedAt = column[LocalDateTime]("failed_at")

    def * = (reportId, emailAddress, errorStatus, errorText, failedAt) <> (ConfirmationFailedRow.tupled, ConfirmationFailedRow.unapply)
  }

  lazy val confirmationFailedTable = TableQuery[ConfirmationFailedTable]

  type ConfirmationSentQuery = Query[ConfirmationSentTable, ConfirmationSentRow, Seq]

  class ConfirmationSentTable(tag: Tag) extends Table[ConfirmationSentRow](tag, "confirmation_sent") {
    def reportId = column[ReportId]("report_id", O.Length(IdType.length))

    def reportIdFK = foreignKey("confirmationsent_report_fk", reportId, reportHeaderTable)(_.id, onDelete = ForeignKeyAction.Cascade)

    def reportIdIndex = index("confirmationsent_report_idx", reportId, unique = true)

    def emailAddress = column[String]("email_address", O.Length(255))

    def emailBody = column[String]("email_body", O.Length(4096))

    def notificationId = column[String]("notification_id", O.Length(IdType.length))

    def sentAt = column[LocalDateTime]("sent_at")

    def * = (reportId, emailAddress, emailBody, notificationId, sentAt) <> (ConfirmationSentRow.tupled, ConfirmationSentRow.unapply)
  }

  lazy val confirmationSentTable = TableQuery[ConfirmationSentTable]

  type ConfirmationPendingQuery = Query[ConfirmationPendingTable, ConfirmationPendingRow, Seq]

  class ConfirmationPendingTable(tag: Tag) extends Table[ConfirmationPendingRow](tag, "confirmation_pending") {
    def reportId = column[ReportId]("report_id", O.Length(IdType.length))

    def reportIdFK = foreignKey("confirmationpending_report_fk", reportId, reportHeaderTable)(_.id, onDelete = ForeignKeyAction.Cascade)

    def reportIdIndex = index("confirmationpending_report_idx", reportId, unique = true)

    def emailAddress = column[String]("email_address", O.Length(255))

    def url = column[String]("url", O.Length(255))

    def retryCount = column[Int]("retry_count")

    def lastErrorStatus = column[Option[Int]]("last_error_status")

    def lastErrorText = column[Option[String]]("last_error_text", O.Length(2048))

    def lockedAt = column[Option[LocalDateTime]]("locked_at")

    def * = (reportId, emailAddress, url, retryCount, lastErrorStatus, lastErrorText, lockedAt) <> (ConfirmationPendingRow.tupled, ConfirmationPendingRow.unapply)
  }

  lazy val confirmationPendingTable = TableQuery[ConfirmationPendingTable]

  override def schema = super.schema ++ confirmationPendingTable.schema ++ confirmationSentTable.schema ++ confirmationFailedTable.schema
}