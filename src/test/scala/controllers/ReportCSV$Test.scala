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
