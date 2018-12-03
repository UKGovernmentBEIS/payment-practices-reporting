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

import controllers.FormPageDefs.FormName
import models.CompaniesHouseId
import play.api.data.Form
import play.api.libs.json.{JsObject, JsValue}
import play.api.mvc.{Call, Request}
import play.twirl.api.Html
import scala.concurrent.Future

/**
  * @tparam T the type of the form that is being processed by this page
  * @tparam N the type of the form name
  */
case class FormHandler[T, N <: FormName](
  formName: N,
  form: Form[T],
  private val renderPageFunction: (Html, CompaniesHouseId, Boolean, Option[JsObject]) => (Form[T]) => Html,
  private val callPageFunction: (CompaniesHouseId, Boolean) => Call
) {
  def bind(implicit request: Request[Map[String, Seq[String]]]): FormHandler[T, N] = copy(form = form.bindForm)

  def bind(jsValue: JsValue): FormHandler[T, N] = copy(form = form.bind(jsValue))

  def bind(data: Map[String, String]): FormHandler[T, N] = copy(form = form.bind(data))

  def callPage(companiesHouseId: CompaniesHouseId, change: Boolean): Call =
    callPageFunction(companiesHouseId, change)

  def renderPage(reportPageHeader: Html, companiesHouseId: CompaniesHouseId, change: Boolean, session: Option[JsObject] = Option.empty[JsObject]): Html =
    renderPageFunction(reportPageHeader, companiesHouseId, change, session)(form)
}
