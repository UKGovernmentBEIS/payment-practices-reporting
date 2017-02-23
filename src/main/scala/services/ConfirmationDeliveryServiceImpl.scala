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

package services

import javax.inject.Inject

import config.Config
import db.ConfirmationPendingRow
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import slicks.modules.{ConfirmationRepo, FiledReport}
import uk.gov.service.notify.NotificationClientException

import scala.concurrent.{ExecutionContext, Future}

class ConfirmationDeliveryServiceImpl @Inject()(confirmationRepo: ConfirmationRepo, mailer: NotifyService) extends ConfirmationDeliveryService {
  val templateId = Config.config.notifyService.templateId
  val df = DateTimeFormat.forPattern("d MMMM YYYY")

  def attemptDelivery(implicit ec: ExecutionContext): Future[Option[DeliveryOutcome]] = {
    confirmationRepo.findUnconfirmedAndLock().flatMap {
      case Some((confirmation, report)) =>
        mailer.sendEmail(templateId, confirmation.emailAddress, buildParams(confirmation, report)).flatMap { response =>
          confirmationRepo.confirmationSent(report.header.id, LocalDateTime.now, response)
            .map(_ => Some(ConfirmationSent(report.header.id)))
        }.recoverWith {
          case nex: NotificationClientException =>
            confirmationRepo.confirmationFailed(report.header.id, LocalDateTime.now, nex)
              .map(_ => Some(ConfirmationFailed(report.header.id)))
        }

      case _ => Future.successful(None)
    }
  }

  private def buildParams(row: ConfirmationPendingRow, report: FiledReport) = {
    Map[String, String](
      "companyName" -> report.header.companyName,
      "companieshouseidentifier" -> report.header.companyId.id,
      "startdate" -> df.print(report.period.startDate),
      "enddate" -> df.print(report.period.endDate),
      "reportid" -> row.reportId.id.toString,
      "reporturl" -> row.url
    )
  }
}
