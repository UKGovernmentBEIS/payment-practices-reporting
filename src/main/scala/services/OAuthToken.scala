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

import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

case class OAuthToken(accessToken: String, accessTokenExpiry: LocalDateTime, refreshToken: String) {
  def isExpired = LocalDateTime.now.isAfter(accessTokenExpiry)
}

object OAuthToken {
  private val pattern = "yyyy-MM-dd HH:mm:ss"
  val df = DateTimeFormat.forPattern(pattern)

  implicit val ldtFormat = new Format[LocalDateTime] {
    override def reads(json: JsValue): JsResult[LocalDateTime] = json.validate[JsString].flatMap { js =>
      Try {
        df.parseLocalDateTime(js.value)
      } match {
        case Success(ldt) => JsSuccess(ldt)
        case Failure(t) => JsError(s"could not parse $js as a LocalDateTime with pattern $pattern")
      }
    }

    override def writes(d: LocalDateTime): JsValue = JsString(df.print(d))
  }

  implicit val fmt = Json.format[OAuthToken]
}