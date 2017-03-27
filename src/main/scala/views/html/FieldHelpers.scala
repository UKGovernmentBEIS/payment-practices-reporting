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

package views.html

import play.api.data.{Field, FormError}
import play.api.i18n.{Lang, MessagesApi}

object FieldHelpers {
  def errorMessage(field: Field)(implicit messages: MessagesApi) =
    field.error.map(e => messages(e.message, e.args: _*))

  def errorClass(field: Field) = if (field.hasErrors) "error" else ""

  def messageFor(key: String)(implicit messages: MessagesApi, lang: Lang): Option[String] =
    if (messages.isDefinedAt(key)) Some(messages(key)) else None

  def errorLinkText(err: FormError)(implicit messages: MessagesApi, lang: Lang): String = {
    val fieldName = messageFor(s"field.${err.key}.name").orElse(messageFor(s"field.${err.key}.label")).getOrElse(err.key)
    val args = err.args ++ Seq(fieldName)
    messages(s"${err.message}.description", args:_*)
  }

}
