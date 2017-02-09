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
import play.api.Logger
import play.api.mvc.{Filter, RequestHeader, Result}
import services.RestService.{JsonParseException, RestFailure}

import scala.concurrent.{ExecutionContext, Future}

class RestErrorFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  import play.api.mvc.Results._

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    f(rh).recoverWith {
      case JsonParseException(method, request, response, errs) =>
        Logger.error(s"$method to ${request.url} failed with json parse errors")
        Logger.debug(s"response body is ${response.body}")
        Logger.debug(errs.toString())
        Future.successful(BadGateway(errs.toString()))

      case RestFailure(method, request, response) =>
        Logger.error(s"$method to ${request.url} failed with status ${response.status}")
        Logger.debug(s"response body is ${response.body}")
        Future.successful(BadGateway(request.url))
    }
  }
}
