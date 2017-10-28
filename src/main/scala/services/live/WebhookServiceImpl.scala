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

import cats.Eval.always
import cats.effect.IO
import play.api.Logger
import play.api.libs.json.Json._
import play.api.libs.ws.{WSClient, WSRequest}
import services.WebhookService

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class WebhookServiceImpl @Inject()(ws: WSClient, @Named("WebhookURL") whURL: String)(implicit ec: ExecutionContext) extends WebhookService {
  private val whRequest: Option[WSRequest] = {
    /*
    * By forcing the lazy `uri` value we can trigger any exception caused by an
    * invalid url string and log it eagerly. We'll either end up with a valid
    * `WSRequest`, or log an error and be left with `None`
     */
    val whTry = for {
      req <- Try(ws.url(whURL))
      _ <- Try(req.uri)
    } yield req

    whTry match {
      case Failure(t) =>
        Logger.error(s"Webhook URL is invalid", t)
        None

      case Success(req) => Some(req)
    }
  }

  override def send(text: String): IO[Unit] = {
    whRequest match {
      case Some(req) => IO.fromFuture(always(req.post(obj("username" -> "Publish Payment Report", "text" -> text)).map(_ => ())))
      case None      => IO.unit
    }
  }
}


