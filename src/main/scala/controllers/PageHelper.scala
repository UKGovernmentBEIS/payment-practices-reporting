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

import controllers.QuestionnaireValidations.decisionStateMapping
import org.scalactic.TripleEquals._
import play.api.data.{Form, FormError, Mapping}
import play.api.mvc.{Call, Request, Result}
import play.twirl.api.Html

import scala.collection.immutable

case class Breadcrumb(href: Call, name: String)

trait PageHelper {
  def page(contents: Html*) = {
    val content = new Html(immutable.Seq[Html](contents: _*))
    views.html.templates.govukTemplateDefaults.render("Payment practices reporting", content)
  }

  val homeBreadcrumb = Breadcrumb(routes.HomeController.index(), "Payment practices reporting")
  val home = breadcrumbs(homeBreadcrumb)

  def breadcrumbs(crumbs: Breadcrumb*): Html = views.html.shared._breadcrumbs(crumbs)

  /**
    * If all the fields are empty then don't report any errors
    */
  def discardErrorsIfEmpty[T](form: Form[T]): Form[T] =
    if (form.data.exists(_._2 !== "")) form else form.discardingErrors

  val todo = controllers.routes.Default.todo()

  def keysFor[T](mapping: Mapping[T]): Seq[String] = mapping.mappings.map(_.key).filterNot(_ === "")

  implicit class MappingSyntax[T](mapping: Mapping[T]) {
    def bindFromRequest(implicit request: Request[_]): Either[Seq[FormError], T] =
      mapping.bind(request.session.data)

    def sessionState(implicit request: Request[_]): Map[String, String] =
      request.session.data.filter { case (k, _) => keysFor(mapping).contains(k) }

    /**
      * The url-encoded form values arrive in the form of a `Map[String, Seq[String]]`. For convenience,
      * reduce this to a `Map[String, String]` (as we only expect one value per form key) and filter out
      * keys that don't relate to the decision state to eliminate noise.
      */
    def stateValues(input: Map[String, Seq[String]]): Map[String, String] =
      input.map { case (k, v) => (k, v.headOption) }
        .collect { case (k, Some(v)) if keysFor(mapping).contains(k) => (k, v) }
  }

  implicit class ResultSyntax(result: Result)(implicit request: Request[_]) {
    def removing[T](mapping: Mapping[T]) = result.removingFromSession(keysFor(mapping): _*)
  }

}
