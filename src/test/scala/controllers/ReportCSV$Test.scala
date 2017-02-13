package controllers

import org.scalatest.{Matchers, WordSpecLike}

class ReportCSV$Test extends WordSpecLike with Matchers {

  "escape" should {
    "double up a double-quote character and quote the string" in {
      val s = """foo " bar"""
      val expected = """"foo "" bar""""

      ReportCSV.escape(s) shouldBe expected
    }

    "double up two double-quote characters and quote the string" in {
      val s = """ "foo" "bar" """
      val expected = """" ""foo"" ""bar"" """"

      ReportCSV.escape(s) shouldBe expected
    }

    "quote a string with a newline" in {
      val s = "foo\nbar"
      val expected = "\"foo\nbar\""
      ReportCSV.escape(s) shouldBe expected
    }

    "quote a string with a comma" in {
      val s = "foo,bar"
      val expected = "\"foo,bar\""
      ReportCSV.escape(s) shouldBe expected
    }
  }

}
