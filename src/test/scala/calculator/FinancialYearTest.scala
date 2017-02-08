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
import org.scalatest.{Matchers, WordSpecLike}

class FinancialYearTest extends WordSpecLike with Matchers {

  "FinancialYear" can {
    "calculate reporting periods" should {
      "give one period for a 9 month year" in {
        val fy = FinancialYear(DateRange(new LocalDate(2017, 4, 6), new LocalDate(2018, 1, 5)))

        fy.reportingPeriods.length shouldBe 1
      }
      "give two periods for a 9 month and a day year" in {
        val range = DateRange(new LocalDate(2017, 4, 6), new LocalDate(2018, 1, 6))
        val months = range.monthsInRange
        val fy = FinancialYear(range)

        fy.reportingPeriods.length shouldBe 2
      }
      "give two periods for a 10 month year" in {
        val fy = FinancialYear(DateRange(new LocalDate(2017, 4, 6), new LocalDate(2018, 2, 5)))

        fy.reportingPeriods.length shouldBe 2
      }
      "give three periods for a 16 month year" in {
        val fy = FinancialYear(DateRange(new LocalDate(2017, 4, 6), new LocalDate(2018, 8, 5)))

        fy.reportingPeriods.length shouldBe 3
      }
    }
  }

}
