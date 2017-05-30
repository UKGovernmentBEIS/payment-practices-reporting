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

import play.api.data.Form
import play.api.mvc.Request
import org.scalactic.TripleEquals._

package object controllers {
  implicit class FormBindingSyntax[T](form: Form[T]) {

    /**
      * The `Form.bindFromRequest` method adds all the values from the map of data to the
      * `Form.data` property. This can include keys that are not in the set of keys that
      * the form expects. This means that when we have data from a form held in hidden fields
      * on another form then `bindFromRequest` will end up putting all the data on both
      * `Form` objects. If we subsequently use `.data` to stash the data for one of the forms
      * then we get values for all forms.
      *
      * This version of `bindForm` ensures that only keys relating to the specific form
      * are added to the `Form.data`.
      */
    def bindForm(implicit request: Request[Map[String, Seq[String]]]): Form[T] = {
      val filteredData = request.body.filter { case (k, v) =>
        form.mapping.mappings.exists(m => m.key === k)
      }
      form.bindFromRequest(filteredData)
    }
  }
}
