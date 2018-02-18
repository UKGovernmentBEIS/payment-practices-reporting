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

package actions

import javax.inject.Inject

import cats.instances.string._
import cats.syntax.eq._
import config.ApiConfig
import play.api.mvc.{ActionBuilder, Request, Result, Results}

import scala.concurrent.{ExecutionContext, Future}

class ProtectedApiAction @Inject()(apiConfig: ApiConfig)(implicit ec: ExecutionContext) extends ActionBuilder[Request] {

  import Results.{ServiceUnavailable, Unauthorized}

  override def invokeBlock[A](request: Request[A], body: Request[A] => Future[Result]): Future[Result] = {
    val auth = request.headers.get("Authorization")

    (auth, apiConfig.token) match {
      case (Some(Bearer(suppliedToken)), Some(configuredToken)) if suppliedToken === configuredToken =>
        body(request).map(_.withHeaders("Access-Control-Allow-Origin" -> "*"))

      case (_, None) => Future.successful(ServiceUnavailable)
      case _         => Future.successful(Unauthorized)
    }
  }
}

object Bearer {
  private val BearerExpr = "Bearer ([\\w.~+/-]+)".r

  def unapply(s: String): Option[String] = s match {
    case BearerExpr(token) => Some(token)
    case _                 => None
  }
}