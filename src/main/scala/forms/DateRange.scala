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

package forms

import org.joda.time.{LocalDate, Months}

case class DateRange(startDate: LocalDate, endDate: LocalDate) {
  def startsOnOrAfter(localDate: LocalDate): Boolean = !startDate.isBefore(localDate)

  val monthsInRange: Int = Months.monthsBetween(startDate, endDate).getMonths + 1

  def splitAt(months: Int): (DateRange, Option[DateRange]) = {
    if (monthsInRange <= months) (this, None)
    else {
      val firstPeriod = DateRange(startDate, startDate.plusMonths(months).minusDays(1))
      val remainder = DateRange(startDate.plusMonths(months), endDate)
      (firstPeriod, Some(remainder))
    }
  }
}
