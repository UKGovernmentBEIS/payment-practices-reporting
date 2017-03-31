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
import org.scalactic.TripleEquals._
import play.api.mvc.{Filter, RequestHeader, Result}
import play.api.{Environment, Logger, Mode}

import scala.concurrent.{ExecutionContext, Future}

class SecurityHeadersFilter @Inject()(env: Environment)(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  override def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    if (env.mode === Mode.Prod) {
      Logger.debug("adding security headers")
      next(rh).map { result =>
        result.withHeaders(
          "X-XSS-Protection" -> "1; mode=block",
          "Strict-Transport-Security" -> "max-age=31536000; includeSubDomains",
          "X-Content-Type-Options" -> "nosniff",
          "Cache-control" -> "no-store, no-cache"
        )
      }
    } else {
      Logger.debug("not adding security headers")
      next(rh)
    }

  }
}
