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
import controllers.PagedLongFormModel.FormName
import play.api.data.Form
import play.api.libs.json.{JsObject, Json}
import services.{SessionId, SessionService}

import scala.concurrent.{ExecutionContext, Future}

trait FormSessionHelpers {
  implicit def ec: ExecutionContext
  def sessionService: SessionService

  implicit def sessionIdFromRequest(implicit request: CompanyAuthRequest[_]): SessionId = request.sessionId

  val formDataSessionKey = "formData"

  def bindFormDataFromSession(formHandler: FormHandler[_])(implicit request: CompanyAuthRequest[_]): Future[FormHandler[_]] = {
    sessionService.get[JsObject](request.sessionId, formDataSessionKey).map {
      case None       => formHandler
      case Some(data) =>
        (data \\ formHandler.formName.entryName).headOption.map { fd =>
          val boundFormHandler = formHandler.bind(fd)
          boundFormHandler
        }.getOrElse(formHandler)
    }
  }

  protected def loadAllFormData(implicit sessionId: SessionId): Future[JsObject] = {
    sessionService.get[JsObject](sessionId, formDataSessionKey).map(_.getOrElse(Json.obj()))
  }

  protected def loadFormData[T](emptyForm: Form[T], key: String)(implicit sessionId: SessionId): Future[Form[T]] =
    sessionService.get[JsObject](sessionId, formDataSessionKey).map {
      case None       => emptyForm
      case Some(data) =>
        (data \\ key).headOption.map(emptyForm.bind).getOrElse(emptyForm)
    }

  protected def checkValidFromSession[T](emptyForm: Form[T], key: String)(implicit sessionId: SessionId): Future[Boolean] =
    sessionService.get[JsObject](sessionId, formDataSessionKey).map {
      case None       => false
      case Some(data) =>
        (data \\ key).headOption.exists(!emptyForm.bind(_).hasErrors)
    }

  protected def saveFormData[T](formName: FormName, form: Form[T])(implicit sessionId: SessionId): Future[Unit] =
    saveFormData(formName.entryName, form)

  protected def saveFormData[T](key: String, form: Form[T])(implicit sessionId: SessionId): Future[Unit] =
    sessionService.get[JsObject](sessionId, formDataSessionKey).map {
      case None    => Json.obj(key -> form.data)
      case Some(o) => o + (key -> Json.toJson(form.data))
    }.flatMap { updatedFormData =>
      sessionService.put(sessionId, formDataSessionKey, updatedFormData)
    }
}
