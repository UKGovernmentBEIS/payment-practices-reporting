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

import play.api.libs.json.Json

import scala.util.Try

case class NotificationClientErrorMessage(to: Option[List[String]] = None, template: Option[List[String]] = None)

case class NotificationClientErrorBody(message: NotificationClientErrorMessage, result: String)

case class NotificationClientError(statusCode: Int, body: NotificationClientErrorBody)


trait NotificationFailure

case class TransientFailure(statusCode: Int, message: String) extends NotificationFailure

case class PermanentFailure(statusCode: Int, message: String) extends NotificationFailure

/**
  * Unfortunately the Java api library that GDS provide turns the error information from
  * the api calls into a string and makes that the message text on the NotificationClientException
  *
  * This object contains functions that attempt to extract the useful information from that string,
  * namely the HTTP status code and a json object containing the error messages
  */
object NotificationClientErrorProcessing {

  import util.matching.Regex

  // http://stackoverflow.com/a/16256935/26129
  implicit class RegexContext(sc: StringContext) {
    def r = new Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }


  implicit val messageReads = Json.reads[NotificationClientErrorMessage]
  implicit val bodyReads = Json.reads[NotificationClientErrorBody]

  def parseNotificationMessage(s: String): Option[NotificationClientError] = {
    s.replaceAll("\\n", "") match {
      case r"Status code: ([0-9]+)$code (\{.+\})$body" => Try {
        NotificationClientError(code.toInt, Json.parse(body).validate[NotificationClientErrorBody].get)
      }.toOption
      case _ =>
        None
    }
  }

  def processError(error: NotificationClientError): NotificationFailure = {
    error.statusCode match {
      case code if code >= 500 && code <= 599 => TransientFailure(code, error.body.message.toString)
      case code => PermanentFailure(code, error.body.message.toString)
    }
  }
}
