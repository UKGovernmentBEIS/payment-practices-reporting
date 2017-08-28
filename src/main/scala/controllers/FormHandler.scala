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

import controllers.PagedLongFormModel.FormName
import play.api.Logger
import play.api.data.Form
import play.api.libs.json.JsValue
import play.api.mvc.{Call, Request}
import play.twirl.api.Html
import services.CompanyDetail

/**
  * @tparam T the type of the form that is being processed by this page
  */
case class FormHandler[T](
  formName: FormName,
  form: Form[T],
  private val renderPageFunction: (Html, CompanyDetail) => (Form[T]) => Html,
  pageCall: (CompanyDetail) => Call,
  nextPageCall: (CompanyDetail) => Call
) {
  def bind(implicit request: Request[Map[String, Seq[String]]]): FormHandler[T] = copy(form = form.bindForm)

  def bind(jsValue: JsValue): FormHandler[T] = {
    Logger.debug(s"trying to bind $jsValue into ${formName.entryName}")
    val boundForm = form.bind(jsValue)
    Logger.debug(s"bound form is $boundForm")
    copy(form = boundForm)
  }

  def bind(data: Map[String, String]): FormHandler[T] = copy(form = form.bind(data))

  def renderPage(reportPageHeader: Html, companyDetail: CompanyDetail): Html =
    renderPageFunction(reportPageHeader, companyDetail)(form)
}
