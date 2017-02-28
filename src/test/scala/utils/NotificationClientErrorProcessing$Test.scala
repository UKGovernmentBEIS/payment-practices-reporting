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

package utils

import org.scalatest.{Matchers, OptionValues, WordSpecLike}

class NotificationClientErrorProcessing$Test extends WordSpecLike with Matchers with OptionValues {

  "parseNotificationMessage" should {
    "correctly parse message" in {
      val message =
        """Status code: 400 {
          |  "message": {
          |    "to": [
          |      "Not a valid email address"
          |    ]
          |  },
          |  "result": "error"
          |}""".stripMargin

      val expectedBody = NotificationClientErrorBody(NotificationClientErrorMessage(Some(List("Not a valid email address")), None), "error")

      val result = NotificationClientErrorProcessing.parseNotificationMessage(message)
      result.value.statusCode shouldBe 400
      result.value.body shouldBe expectedBody

    }
  }

}
