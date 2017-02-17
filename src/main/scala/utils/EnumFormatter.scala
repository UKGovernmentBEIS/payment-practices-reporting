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

package utils

import enumeratum.{Enum, EnumEntry}
import play.api.data.FormError
import play.api.data.format.Formatter

trait EnumFormatter[E <: EnumEntry] {
  self: Enum[E] =>

  implicit val formatter: Formatter[E] = new Formatter[E] {
    override def unbind(key: String, value: E): Map[String, String] = Map(key -> value.entryName)

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], E] =
      data.get(key) match {
        case None => Left(Seq(FormError(key, "no value found")))
        case Some(s) => self.withNameOption(s) match {
          case Some(co) => Right(co)
          case None => Left(Seq(FormError(key, "not a valid enumeration value")))
        }
      }
  }
}
