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

package services.live

import javax.inject.{Inject, Named}

import akka.actor.ActorRef
import cats.effect.IO
import play.api.Logger
import services.{CompanyDetail, EventHandler, WebhookService}

import scala.concurrent.ExecutionContext

class EventHandlerImpl @Inject()(
  webhookService: WebhookService,
  @Named("confirmation-actor") confirmationActor: ActorRef
)(implicit ec: ExecutionContext) extends EventHandler {
  private def logDebug(msg: String)(e: Either[Throwable, Unit]): Unit = e match {
    case Left(t)  => Logger.debug(msg, t)
    case Right(_) => ()
  }

  override def reportPublished(companyDetail: CompanyDetail, reportURL: String): Unit = {
    webhookService.send(s"Report published for ${companyDetail.companyName}\n$reportURL").unsafeRunAsync(logDebug("Error sending webhook message"))
    IO(confirmationActor ! 'poll).unsafeRunAsync(logDebug("Error sending poll message to confirmation actor"))
  }
}
