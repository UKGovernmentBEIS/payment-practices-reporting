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
import config.ServiceConfig
import play.api.libs.json.Json._
import play.api.libs.ws.WSClient
import services.{CompanyDetail, EventHandler}

import scala.concurrent.ExecutionContext

class EventHandlerImpl @Inject()(
  ws: WSClient,
  serviceConfig: ServiceConfig,
  @Named("confirmation-actor") confirmationActor: ActorRef
)(implicit ec: ExecutionContext) extends EventHandler {
  /**
    * If a webhook url is configured then send an event to it.
    *
    * Also send a poll message to the confirmation actor to trigger an email to
    * the user who published the report.
    */
  override def reportPublished(companyDetail: CompanyDetail, reportURL: String): Unit = {
    serviceConfig.webhookURL.map { whURL =>
      val text = s"Report published for ${companyDetail.companyName}\n$reportURL"
      ws.url(whURL).post(obj("username" -> "Publish Payment Report", "text" -> text))
    }

    confirmationActor ! 'poll
  }
}
