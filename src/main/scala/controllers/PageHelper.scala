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

import config.AppConfig
import org.scalactic.TripleEquals._
import play.api.data.Form
import play.api.mvc.Call
import play.twirl.api.Html

import scala.collection.immutable

case class Breadcrumb(href: Call, name: String)

trait PageHelper {
  val appConfig: AppConfig

  import appConfig.config.googleAnalytics

  def page(title: String)(contents: Html*): Html = {
    val content = html(contents: _*)
    views.html.templates.govukTemplateDefaults(title)(content)(googleAnalytics.flatMap(_.code))
  }

  def html(contents: Html*): Html = {
    new Html(immutable.Seq[Html](contents: _*))
  }

  def h1(text: String) = views.html.shared._h1(Html(text))

  val homeBreadcrumb = Breadcrumb(routes.HomeController.index(), "Payment practices reporting")
  val home = breadcrumbs(homeBreadcrumb)

  def breadcrumbs(crumbs: Breadcrumb*): Html = views.html.shared._breadcrumbs(crumbs)

  /**
    * If all the fields are empty then don't report any errors
    */
  def discardErrorsIfEmpty[T](form: Form[T]): Form[T] =
    if (form.data.exists(_._2 !== "")) form else form.discardingErrors

  val todo = controllers.routes.Default.todo()
}
