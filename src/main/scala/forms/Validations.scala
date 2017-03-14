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

import calculator.FinancialYear
import forms.report.ReportConstants
import org.joda.time.LocalDate
import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.{Forms, Mapping}
import utils.YesNo

import scala.util.Try

object Validations {

  val averageWordLength = ReportConstants.averageWordLength

  /**
    * Very simple word-count algorithm - just split at whitespace and count the results.
    * I'm sure we can do better, but do we need to?
    */
  def countWords(s: String) = s.split("\\s+").length

  def minWordConstraint(words: Int): Constraint[String] = Constraint[String]("constraint.minWords", words) { s =>
    require(words >= 0, "string minWords must not be negative")
    if (s == null) Invalid(ValidationError("error.minWords", words))
    else if (countWords(s) >= words) Valid
    else Invalid(ValidationError("error.minWords", words))
  }

  def maxWordConstraint(words: Int): Constraint[String] = Constraint[String]("constraint.maxWords", words) { s =>
    require(words >= 0, "string maxWords must not be negative")
    if (s == null) Invalid(ValidationError("error.maxWords", words))
    else if (countWords(s) <= words) Valid
    else Invalid(ValidationError("error.maxWords", words))
  }

  def words(minWords: Int = 0, maxWords: Int = Int.MaxValue): Mapping[String] = (minWords, maxWords) match {
    case (0, Int.MaxValue) => text
    case (min, Int.MaxValue) => text.verifying(minWordConstraint(min), Constraints.minLength(min))
    case (0, max) => text.verifying(maxWordConstraint(max), Constraints.maxLength(max * averageWordLength))
    case (min, max) =>
      text.verifying(
        minWordConstraint(min), Constraints.minLength(min),
        maxWordConstraint(max), Constraints.maxLength(max * averageWordLength))
  }

  val dateFields: Mapping[DateFields] = mapping(
    "day" -> number,
    "month" -> number,
    "year" -> number
  )(DateFields.apply)(DateFields.unapply)
    .verifying("error.date", fields => validateFields(fields))

  val dateFromFields: Mapping[LocalDate] = dateFields.transform(toDate, fromDate)

  private def validateFields(fields: DateFields): Boolean = Try(toDate(fields)).isSuccess

  /**
    * Warning: Will throw an exception if the fields don't constitute a valid date. This is provided
    * to support the `.transform` call below on the basis that the fields themselves will have already
    * been verified with `validateFields`
    */
  private def toDate(fields: DateFields): LocalDate = new LocalDate(fields.year, fields.month, fields.day)

  private def fromDate(date: LocalDate): DateFields = DateFields(date.getDayOfMonth, date.getMonthOfYear, date.getYear)

  val dateRange: Mapping[DateRange] = mapping(
    "startDate" -> dateFromFields,
    "endDate" -> dateFromFields
  )(DateRange.apply)(DateRange.unapply)
    .verifying("error.endafterstart", dr => dr.endDate.isAfter(dr.startDate))

  val financialYear: Mapping[FinancialYear] = mapping(
    "fy" -> dateRange
  )(FinancialYear.apply)(FinancialYear.unapply)

  val yesNo: Mapping[YesNo] = Forms.of(YesNo.formatter)


}
