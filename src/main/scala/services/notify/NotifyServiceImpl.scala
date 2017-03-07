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

package services.notify

import java.util
import javax.inject.Inject

import akka.actor.ActorSystem
import config.AppConfig
import services.NotifyService
import uk.gov.service.notify.{NotificationClient, NotificationResponse}

import scala.collection.JavaConversions._
import scala.concurrent.Future

class NotifyServiceImpl @Inject()(actorSystem: ActorSystem, appConfig: AppConfig) extends NotifyService {
  val key = appConfig.config.notifyService.apiKey
  implicit val ec = actorSystem.dispatchers.lookup("email-dispatcher")

  override def sendEmail(templateId: String, recipient: String, params: Map[String, String]): Future[NotificationResponse] = {
    val client = new NotificationClient(key)

    val m: util.Map[String, String] = params
    val jParams = new util.HashMap[String, String]()
    jParams.putAll(m)
    Future {
      client.sendEmail(templateId, recipient, jParams)
    }
  }
}
