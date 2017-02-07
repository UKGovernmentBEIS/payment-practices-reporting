package forms

import org.joda.time.LocalDate
import org.scalatest.{Matchers, WordSpecLike}

class DateRangeTest extends WordSpecLike with Matchers {

  "DateRange" can {
    "calculate months in range" should {
      "give 6 months" in {
        DateRange(new LocalDate(2017,4,6), new LocalDate(2017, 10, 5)).monthsInRange shouldBe 6
      }
      "give 7 months" in {
        DateRange(new LocalDate(2017,4,6), new LocalDate(2017, 10, 6)).monthsInRange shouldBe 7
      }
    }
  }

}
