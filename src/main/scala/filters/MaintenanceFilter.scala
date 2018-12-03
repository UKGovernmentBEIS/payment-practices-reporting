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

import akka.stream.Materializer
import config.{AppConfig, ServiceConfig}
import javax.inject.Inject
import play.api.Logger
import play.api.mvc.Results.TemporaryRedirect
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

class MaintenanceFilter @Inject()(appConfig: AppConfig, serviceConfig: ServiceConfig)(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  import appConfig.config

  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {

      if (!serviceConfig.maintenance.contains(true) || (requestHeader.uri.startsWith("/maintenance") || requestHeader.uri.startsWith("/assets") || requestHeader.uri.startsWith("/public"))) nextFilter(requestHeader)
      else {
        Future.successful(TemporaryRedirect("/maintenance"))
      }
    }
}
