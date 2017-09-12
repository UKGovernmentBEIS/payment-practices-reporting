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

package filters

import javax.inject.Inject

import akka.stream.Materializer
import config.AppConfig
import play.api.Logger
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Provides logging of the requests, with timings. Logging can be turned on and off with a config
  * parameter of `logRequests`. Requests for static assets (i.e. urls starting with "/assets") can
  * be independently turned on/off with a config parameter of `logAssets`, as they can lead to a lot
  * of noise in the log files.
  */
class LoggingFilter @Inject()(appConfig: AppConfig)(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  import appConfig.config

  private lazy val logAssets   = config.logAssets.getOrElse(false)
  private lazy val logRequests = config.logRequests.getOrElse(false)

  def apply(nextFilter: RequestHeader => Future[Result])
    (requestHeader: RequestHeader): Future[Result] = {

    if ((requestHeader.uri.startsWith("/assets") || requestHeader.uri.startsWith("/public")) && !logAssets) nextFilter(requestHeader)
    else {
      val startTime = System.currentTimeMillis

      if (logRequests) Logger.trace(s"${requestHeader.method} ${requestHeader.uri} received...")

      nextFilter(requestHeader).map { result =>

        val endTime = System.currentTimeMillis
        val requestTime = endTime - startTime

        if (logRequests) Logger.info(f"method=${requestHeader.method}%-6s status=${result.header.status} time=$requestTime%7sms url=${requestHeader.uri}")

        result.withHeaders("Request-Time" -> requestTime.toString)
      }
    }
  }
}