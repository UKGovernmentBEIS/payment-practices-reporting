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
import db.ConfirmationEmailRow
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import play.api.Logger
import services.NotifyService
import slicks.modules.{ConfirmationEmailRepo, FiledReport, ReportRepo}

import scala.concurrent.Future
import scala.util.Success

class ConfirmationEmailActor @Inject()(reportRepo: ReportRepo, confirmationRepo: ConfirmationEmailRepo, mailer: NotifyService) extends Actor {
  implicit val ec = context.dispatcher

  Logger.debug("Started ConfirmationEmailActor")

  val templateId = Config.config.notifyService.templateId

  val df = DateTimeFormat.forPattern("d MMMM YYYY")

  override def preStart(): Unit = self ! 'start

  def receive = {
    case 'start => self ! 'poll

    case 'poll =>
      Logger.trace(s"polling for unconfirmed reports")

      confirmationRepo.findUnconfirmedAndLock().onComplete {
        case Success(Some(row)) =>
          Logger.debug(s"Found unsent confirmation row for ${row.reportId}")
          reportRepo.findFiled(row.reportId).flatMap {
            case Some(report) =>
              Logger.debug(s"Sending email for ${row.reportId} to ${row.emailAddress}")
              mailer.sendEmail(templateId, row.emailAddress, buildParams(row, report)).map { response =>
                Logger.debug(s"Response is ${response.getBody}")
                Logger.debug(s"Notification id is ${response.getNotificationId}")
                confirmationRepo.sentAt(row.reportId, LocalDateTime.now)
              }.recover {
                case t => Logger.debug("Sending failed", t)
              }

            case None =>
              Logger.error(s"Confirmation row for ${row.reportId} found, but no filed report was found!")
              Future.successful(())
          }
          self ! 'poll

        case _ => Thread.sleep(10000); self ! 'poll
      }

    case x => Logger.error(s"received $x")
  }

  private def buildParams(row: ConfirmationEmailRow, report: FiledReport) = {
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
