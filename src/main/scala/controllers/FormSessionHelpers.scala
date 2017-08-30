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
import controllers.FormPageModels._
import org.scalactic.TripleEquals._
import play.api.Logger
import play.api.data.Form
import play.api.libs.json.{JsObject, Json}
import services.{SessionId, SessionService}

import scala.concurrent.{ExecutionContext, Future}

trait FormSessionHelpers {
  implicit def ec: ExecutionContext
  def sessionService: SessionService

  implicit def sessionIdFromRequest(implicit request: CompanyAuthRequest[_]): SessionId = request.sessionId

  val formDataSessionKey = "formData"

  private def bindPage[T, N <: FormName](data: JsObject, handler: FormHandler[T, N]): FormStatus[T, N] = {
    val boundHandler = handler.bind((data \\ handler.formName.entryName).headOption.getOrElse(Json.obj()))
    boundHandler.form.value match {
      case Some(v) => FormIsOk(boundHandler, v)
      case None    => if (boundHandler.form.data.isEmpty && boundHandler.form.value.isEmpty) FormIsBlank(boundHandler)
                      else FormHasErrors(boundHandler)
    }
  }

  protected def bindAllPages[N <: FormName](formHandlers: Seq[FormHandler[_, N]])(implicit request: CompanyAuthRequest[_]): Future[FormStatus[_, N]] = {
    loadAllFormData.map { data =>
      formHandlers.foldLeft(Seq.empty[FormStatus[_, N]]) { (results, handler) =>
        results.headOption match {
          case r@Some(FormIsBlank(_))   =>
            Logger.debug(r.toString)
            results
          case r@Some(FormHasErrors(_)) =>
            Logger.debug(r.toString)
            results
          case _                        => bindPage(data, handler) +: results
        }
      }.head
    }
  }

  /**
    * Bind pages up to including the page with the given `formName`, returning the first result that
    * is empty or fails validation, or an Ok result for the named form.
    */
  protected def bindUpToPage[N <: FormName](formHandlers: Seq[FormHandler[_, N]], formName: N)(implicit request: CompanyAuthRequest[_]): Future[FormStatus[_, N]] = {
    val (handlersToBind, _) = formHandlers.splitAt(formHandlers.indexWhere(_.formName === formName) + 1)

    loadAllFormData.map { data =>
      handlersToBind.foldLeft(Seq.empty[FormStatus[_, N]]) { (results, handler) =>
        results.headOption match {
          case Some(FormHasErrors(_)) | Some(FormIsBlank(_)) => results
          case _                                             => bindPage(data, handler) +: results
        }
      }.head
    }
  }

  private def loadAllFormData(implicit sessionId: SessionId): Future[JsObject] = {
    sessionService.get[JsObject](sessionId, formDataSessionKey).map(_.getOrElse(Json.obj()))
  }

  protected def loadFormData[T](emptyForm: Form[T], formName: FormName)(implicit sessionId: SessionId): Future[Form[T]] =
    sessionService.get[JsObject](sessionId, formDataSessionKey).map {
      case None       => emptyForm
      case Some(data) =>
        (data \\ formName.entryName).headOption.map(emptyForm.bind).getOrElse(emptyForm)
    }

  protected def checkValidFromSession[T](emptyForm: Form[T], key: String)(implicit sessionId: SessionId): Future[Boolean] =
    sessionService.get[JsObject](sessionId, formDataSessionKey).map {
      case None       => false
      case Some(data) =>
        (data \\ key).headOption.exists(!emptyForm.bind(_).hasErrors)
    }

  protected def saveFormData[T](formName: FormName, form: Form[T])(implicit sessionId: SessionId): Future[Unit] =
    saveFormData(formName.entryName, form)

  private def saveFormData[T](key: String, form: Form[T])(implicit sessionId: SessionId): Future[Unit] =
    sessionService.get[JsObject](sessionId, formDataSessionKey).map {
      case None    => Json.obj(key -> form.data)
      case Some(o) => o + (key -> Json.toJson(form.data))
    }.flatMap { updatedFormData =>
      sessionService.put(sessionId, formDataSessionKey, updatedFormData)
    }
}
