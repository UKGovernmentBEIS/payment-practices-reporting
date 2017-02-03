package questionairre

import enumeratum.EnumEntry.Lowercase
import enumeratum._
import questionairre.Answer.{No, Unanswered, Yes}

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