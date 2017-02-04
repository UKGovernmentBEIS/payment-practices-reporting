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

package questionnaire

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.json.Json
import questionnaire.Answer.{No, Unanswered, Yes}

sealed trait Answer extends EnumEntry with Lowercase

object Answer extends Enum[Answer] with PlayJsonEnum[Answer] {
  override def values = findValues

  case object Yes extends Answer

  case object No extends Answer

  case object Unanswered extends Answer

}

case class AnswerGroup(turnover: Answer, balanceSheet: Answer, employees: Answer) {
  def score: Int = Seq(turnover, balanceSheet, employees).count(_ == Answer.Yes)

  def nextQuestion(questionGroup: QuestionGroup): Option[Question] = (turnover, balanceSheet, employees) match {
    case (Unanswered, _, _) => Some(questionGroup.turnoverQuestion)
    case (_, Unanswered, _) => Some(questionGroup.balanceSheetQuestion)
    case (Yes, No, Unanswered) | (No, Yes, Unanswered) => Some(questionGroup.employeesQuestion)
    case _ => None
  }
}

object AnswerGroup {
  val empty: AnswerGroup = AnswerGroup(Unanswered, Unanswered, Unanswered)

  implicit val format = Json.format[AnswerGroup]
}