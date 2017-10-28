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

package services.mocks

import services.NotifyService
import uk.gov.service.notify.SendEmailResponse

import scala.concurrent.{ExecutionContext, Future}

class MockNotify extends NotifyService {

  val json =
    """
      |{
      |  "id":"36ccf3ec-bbd6-11e7-abc4-cec278b6b50a",
      |  "content":{
      |    "subject":"report published",
      |    "body":"Your report has been published.",
      |  },
      |  "template": {
      |    "id":"36ccf3ec-bbd6-11e7-abc4-cec278b6b50a",
      |    "version":1,
      |    "uri" :"http://localhost"
      |  }
      |}
    """.stripMargin

  override def sendEmail(recipient: String, params: Map[String, String])(implicit ec: ExecutionContext): Future[SendEmailResponse] = {
    Future.successful(new SendEmailResponse(json))
  }
}
