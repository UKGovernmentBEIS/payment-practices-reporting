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

package controllers

import actions.CompanyAuthRequest
import play.api.Logger
import play.api.data.Form
import play.api.libs.json.JsObject
import services.{SessionId, SessionService}

import scala.concurrent.{ExecutionContext, Future}

trait SessionHelpers {
  implicit def ec: ExecutionContext
  def sessionService: SessionService

  implicit def sessionIdFromRequest(implicit request: CompanyAuthRequest[_]): SessionId = request.sessionId

  protected def bindFromSession[T](emptyForm: Form[T], key: String)(implicit sessionId: SessionId): Future[Form[T]] =
    sessionService.get[JsObject](sessionId, key).map {
      case None       => emptyForm
      case Some(data) => emptyForm.bind(data)
    }

  protected def checkValidFromSession[T](emptyForm: Form[T], key: String)(implicit sessionId: SessionId): Future[Boolean] =
    sessionService.get[JsObject](sessionId, key).map {
      case None       => false
      case Some(data) =>
        val boundForm = emptyForm.bind(data)
        Logger.debug(boundForm.errors.toString)
        !boundForm.hasErrors
    }

}
