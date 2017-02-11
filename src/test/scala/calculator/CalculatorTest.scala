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
import org.joda.time.format.DateTimeFormat
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{Matchers, WordSpecLike}

import scala.language.implicitConversions

class CalculatorTest extends WordSpecLike with Matchers with TableDrivenPropertyChecks {

  import CalculatorTestData._

  val table = Table("test records", testRecords: _*)

  "Calculator" should {
    "calculate periods" in {
      forAll(table) { record =>
        Calculator(FinancialYear(record.input)).reportingPeriods shouldBe record.expected
      }
    }
  }
}


object CalculatorTestData {

  case class TestRecord(input: DateRange, expected: Seq[ReportingPeriod])

  val df = DateTimeFormat.forPattern("yyyy-M-d")

  implicit def toDate(s: String): LocalDate = df.parseLocalDate(s)

  val testRecords: Seq[TestRecord] = Seq[(LocalDate, LocalDate, Seq[(LocalDate, LocalDate)])](
    ("2020-1-1", "2020-12-31", Seq(("2020-1-1", "2020-6-30"), ("2020-7-1", "2020-12-31"))),
    ("2017-1-1", "2017-12-31", Seq(("2018-1-1", "2018-6-30"), ("2018-7-1", "2018-12-31"))),
    ("2016-1-1", "2016-12-31", Seq(("2018-1-1", "2018-6-30"), ("2018-7-1", "2018-12-31"))),
    ("2017-9-1", "2018-8-31", Seq(("2017-9-1", "2018-2-28"), ("2018-3-1", "2018-8-31"))),
    ("2018-1-1", "2018-9-30", Seq(("2018-1-1", "2018-9-30"))),
    ("2018-1-1", "2019-3-31", Seq(("2018-1-1", "2018-6-30"), ("2018-7-1", "2019-3-31"))),
    ("2018-1-1", "2019-4-1", Seq(("2018-1-1", "2018-6-30"), ("2018-7-1", "2018-12-31"), ("2019-1-1", "2019-4-1"))),
    ("2018-1-1", "2019-9-30", Seq(("2018-1-1", "2018-6-30"), ("2018-7-1", "2018-12-31"), ("2019-1-1", "2019-9-30"))),
    ("2018-1-1", "2019-10-1", Seq(("2018-1-1", "2018-6-30"), ("2018-7-1", "2018-12-31"), ("2019-1-1", "2019-10-1"))),
    ("2018-3-1", "2019-2-28", Seq(("2018-3-1", "2018-8-31"), ("2018-9-1", "2019-2-28"))),
    ("2017-8-31", "2019-8-30", Seq(("2017-8-31", "2018-2-27"), ("2018-2-28", "2018-8-30"), ("2018-8-31", "2019-8-30"))),
    ("2017-8-28", "2019-8-27", Seq(("2017-8-28", "2018-2-27"), ("2018-2-28", "2018-8-27"), ("2018-8-28", "2019-8-27")))
  ).map {
    case (s, e, periods) => TestRecord(DateRange(s, e), periods.map(p => ReportingPeriod(DateRange(p._1, p._2))))
  }

}