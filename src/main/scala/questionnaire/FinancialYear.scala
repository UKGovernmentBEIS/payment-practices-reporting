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

package questionnaire

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import questionnaire.FinancialYear.{First, Second, ThirdOrLater}
import utils.EnumFormatter

sealed trait FinancialYear extends EnumEntry with Lowercase {
  def fold[T](first: => T, second: => T, third: => T): T = this match {
    case First        => first
    case Second       => second
    case ThirdOrLater => third
  }
}

object FinancialYear extends Enum[FinancialYear] with PlayJsonEnum[FinancialYear] with EnumFormatter[FinancialYear] {
  //noinspection TypeAnnotation
  override def values = findValues

  case object First extends FinancialYear

  case object Second extends FinancialYear

  case object ThirdOrLater extends FinancialYear
}

