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

import cats.syntax.either._
import play.api.data.{Form, FormError}
import play.api.mvc.PathBindable
import play.twirl.api.Html
import questionnaire.Questions.questionMap
import utils.YesNo

sealed trait Question {
  def id: Int

  def choices: Seq[Choice]

  def inline: Boolean

  def fieldKey: String

  def textKey: String

  def hintKey: Option[String]

  def detailText: Option[Html]

  def bindAnswer(data: Map[String, Seq[String]]): Either[FormError, Answer]
}

object Question {
  implicit val questionPathBindable: PathBindable[Question] = new PathBindable[Question] {
    override def bind(key: String, value: String): Either[String, Question] = implicitly[PathBindable[Int]].bind(key, value).flatMap { id =>
      questionMap.get(id) match {
        case None    => Left(s"No question found with id $id")
        case Some(q) => Right(q)
      }
    }

    override def unbind(key: String, q: Question): String = q.id.toString
  }
}

case class FinancialYearQuestion(id: Int, fieldKey: String, textKey: String, hintKey: Option[String], detailText: Option[Html] = None) extends Question {

  override def choices =
    Seq(
      Choice("choice.first", FinancialYear.First.entryName),
      Choice("choice.second", FinancialYear.Second.entryName),
      Choice("choice.third", FinancialYear.ThirdOrLater.entryName)
    )

  override def inline = false

  import controllers.QuestionnaireValidations._

  val emptyForm = Form(yearAnswer)

  override def bindAnswer(data: Map[String, Seq[String]]): Either[FormError, FinancialYearAnswer] =
    emptyForm.bindFromRequest(data).fold(
      errs => Left(errs.error("answer").get),
      answer => Right(answer)
    )
}

case class YesNoQuestion(id: Int, fieldKey: String, textKey: String, hintKey: Option[String], detailText: Option[Html] = None) extends Question {

  import YesNo._

  override def choices: Seq[Choice] = Seq(Choice("Yes", Yes.entryName), Choice("No", No.entryName))

  override def inline = true

  import controllers.QuestionnaireValidations._

  val emptyForm = Form(yesNoAnswer)

  override def bindAnswer(data: Map[String, Seq[String]]): Either[FormError, YesNoAnswer] =
    emptyForm.bindFromRequest(data).fold(
      errs => Left(errs.error("answer").get),
      answer => Right(answer)
    )
}
