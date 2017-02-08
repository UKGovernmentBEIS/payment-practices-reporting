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
