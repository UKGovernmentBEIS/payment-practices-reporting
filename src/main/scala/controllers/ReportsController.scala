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

package controllers

import javax.inject.Inject

import actions.ProtectedApiAction
import models.ReportId
import org.joda.time.LocalDateTime
import play.api.libs.json.{Json, Reads}
import play.api.mvc._
import services._

import scala.concurrent.ExecutionContext

case class ArchiveRequest(timestamp: Option[LocalDateTime], comment: Option[String])
object ArchiveRequest {
  implicit val jodaDateReads: Reads[LocalDateTime]  = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ").map(_.toLocalDateTime)
  implicit val reads        : Reads[ArchiveRequest] = Json.reads
}

class ReportsController @Inject()(
  val reportService: ReportService,
  protectedAction: ProtectedApiAction
)(implicit val ec: ExecutionContext)
  extends Controller {

  //noinspection TypeAnnotation
  def archive(reportId: ReportId) = protectedAction.async(BodyParsers.parse.json[ArchiveRequest]) { implicit request =>
    val timestamp = request.body.timestamp.getOrElse(LocalDateTime.now)
    val comment = request.body.comment.getOrElse(s"Archived via an api call.")

    reportService.archive(reportId, timestamp, comment).map {
      case ArchiveResult.Archived        => NoContent
      case ArchiveResult.AlreadyArchived => Conflict("The report is already archived.")
      case ArchiveResult.NotFound        => NotFound
    }
  }
  //noinspection TypeAnnotation
  def unarchive(reportId: ReportId) = protectedAction.async(BodyParsers.parse.json[ArchiveRequest]) { implicit request =>
    val timestamp = request.body.timestamp.getOrElse(LocalDateTime.now)
    val comment = request.body.comment.getOrElse(s"Un-archived via an api call.")

    reportService.unarchive(reportId, timestamp, comment).map {
      case UnarchiveResult.Unarchived  => NoContent
      case UnarchiveResult.NotArchived => Conflict("The report is not currently archived.")
      case UnarchiveResult.NotFound    => NotFound
    }
  }
}
