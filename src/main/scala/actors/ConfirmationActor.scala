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

package actors

import javax.inject.Inject

import akka.actor.Actor
import config.Config
import db.ConfirmationPendingRow
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import play.api.Logger
import services.NotifyService
import slicks.modules.{ConfirmationRepo, FiledReport, ReportRepo}
import uk.gov.service.notify.NotificationClientException

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

class ConfirmationActor @Inject()(reportRepo: ReportRepo, confirmationRepo: ConfirmationRepo, mailer: NotifyService) extends Actor {
  implicit val ec = context.dispatcher

  context.system.scheduler.schedule(1 second, 10 seconds, self, 'poll)

  Logger.debug("Started ConfirmationActor")

  val templateId = Config.config.notifyService.templateId

  val df = DateTimeFormat.forPattern("d MMMM YYYY")

  def receive = {
    case 'poll =>
      Logger.trace(s"polling for unconfirmed reports")

      confirmationRepo.findUnconfirmedAndLock().onComplete {
        case Success(Some((confirmation, report))) =>
          mailer.sendEmail(templateId, confirmation.emailAddress, buildParams(confirmation, report)).map { response =>
            confirmationRepo.confirmationSent(report.header.id, LocalDateTime.now, response)
          }.recover {
            case nex: NotificationClientException =>
              confirmationRepo.confirmationFailed(report.header.id, LocalDateTime.now, nex)
          }
          // Send another poll message immediately in case there are more
          // confirmations pending
          self ! 'poll

        case _ => // no action
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
