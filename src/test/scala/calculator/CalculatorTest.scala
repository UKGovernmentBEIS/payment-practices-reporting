package calculator

import forms.DateRange
import org.joda.time.LocalDate
import org.scalatest.{Matchers, WordSpecLike}

class CalculatorTest extends WordSpecLike with Matchers {

  "Calculator" should {
    "calculate periods" in {
      val calc = Calculator(FinancialYear(DateRange(new LocalDate(2017, 1, 1), new LocalDate(2017, 12, 31))))

      calc.reportingPeriods shouldBe Seq(
        ReportingPeriod(DateRange(new LocalDate(2018, 1, 1), new LocalDate(2018, 6, 30))),
        ReportingPeriod(DateRange(new LocalDate(2018, 7, 1), new LocalDate(2018, 12, 31)))
      )
    }
  }
}
