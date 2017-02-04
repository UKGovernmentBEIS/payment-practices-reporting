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

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

package object questionnaire {

  sealed trait YesNo extends EnumEntry with Lowercase

  object YesNo extends Enum[YesNo] with PlayJsonEnum[YesNo] {
    override def values = findValues

    case object Yes extends YesNo

    case object No extends YesNo

  }

  sealed trait FinancialYear extends EnumEntry with Lowercase

  object FinancialYear extends Enum[FinancialYear] with PlayJsonEnum[FinancialYear] {
    override def values = findValues

    case object First extends FinancialYear

    case object Second extends FinancialYear

    case object ThirdOrLater extends FinancialYear

  }

}
