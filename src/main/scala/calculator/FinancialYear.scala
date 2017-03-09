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

package calculator

import forms.DateRange
import org.joda.time.LocalDate

/**
  * A `FinancialYear` wraps a `DateRange` and provides methods for calculating
  * reporting periods and the next financial year, etc.
  */
case class FinancialYear(dates: DateRange) {

  def startsOnOrAfter(cutoff: LocalDate): Boolean = dates.startsOnOrAfter(cutoff)

  def nextYear: FinancialYear =
    FinancialYear(DateRange(dates.endDate.plusDays(1), dates.endDate.plusYears(1)))

  def firstYearOnOrAfter(cutoff: LocalDate): FinancialYear =
    if (startsOnOrAfter(cutoff)) this else nextYear.firstYearOnOrAfter(cutoff)

  /**
    * If the financial year is:
    * 9 months or less - single reporting period covering the whole financial year
    * 9 months and a day up to 15 months - split into 6-month period and a remainder period
    * 15 months and a day or more - split into 2 6-month periods and a remainder period
    */
  def reportingPeriods: Seq[ReportingPeriod] = {
    val ranges: Seq[DateRange] = dates.monthsInRange match {
      case n if n <= 9 => Seq(dates)

      case n if n <= 15 =>
        val second = DateRange(dates.addMonthsWithStickyEnd(dates.startDate, 6), dates.endDate)
        val first = DateRange(dates.startDate, second.startDate.minusDays(1))
        Seq(first, second)

      case _ =>
        val third = DateRange(dates.startDate.plusYears(1), dates.endDate)
        val second = DateRange(dates.addMonthsWithStickyEnd(dates.startDate, 6), third.startDate.minusDays(1))
        val first = DateRange(dates.startDate, second.startDate.minusDays(1))
        Seq(first, second, third)
    }
    ranges.map(ReportingPeriod)
  }
}
