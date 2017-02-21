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
import db.ConfirmationEmailRow
import models.ReportId
import org.joda.time.LocalDateTime
import slicks.DBBinding

trait ConfirmationEmailModule extends DBBinding {
  self: ReportModule with PgDateSupportJoda =>

  import api._

  type ConfirmationEmailQuery = Query[ConfirmationEmailTable, ConfirmationEmailRow, Seq]

  class ConfirmationEmailTable(tag: Tag) extends Table[ConfirmationEmailRow](tag, "confirmation_email") {
    def reportId = column[ReportId]("report_id", O.Length(IdType.length))

    def reportIdFK = foreignKey("confirmationemail_report_fk", reportId, reportHeaderTable)(_.id, onDelete = ForeignKeyAction.Cascade)

    // Only one confirmation row per report
    def reportIdIndex = index("confirmationemail_report_idx", reportId, unique = true)

    def emailAddress = column[Option[String]]("email_address", O.Length(255))

    def sentAt = column[Option[LocalDateTime]]("sent_at")

    def lockedAt = column[Option[LocalDateTime]]("locked_at")

    def * = (reportId, emailAddress, sentAt, lockedAt) <> (ConfirmationEmailRow.tupled, ConfirmationEmailRow.unapply)
  }

  lazy val confirmationEmailTable = TableQuery[ConfirmationEmailTable]

  override def schema = super.schema ++ confirmationEmailTable.schema
}