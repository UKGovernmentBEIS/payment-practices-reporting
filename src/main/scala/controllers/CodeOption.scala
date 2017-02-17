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

import enumeratum.EnumEntry.Lowercase
import enumeratum._
import play.api.data.FormError
import play.api.data.format.Formatter

sealed trait CodeOption extends EnumEntry with Lowercase

object CodeOption extends Enum[CodeOption] {
  override def values = findValues

  case object Colleague extends CodeOption

  case object Register extends CodeOption

  implicit val codeOptionsFormatter: Formatter[CodeOption] = new Formatter[CodeOption] {
    override def unbind(key: String, value: CodeOption): Map[String, String] = Map(key -> value.entryName)

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], CodeOption] =
      data.get(key) match {
        case None => Left(Seq(FormError(key, "no value found")))
        case Some(s) => CodeOption.withNameOption(s) match {
          case Some(co) => Right(co)
          case None => Left(Seq(FormError(key, "not a valid code option")))
        }
      }
  }
}