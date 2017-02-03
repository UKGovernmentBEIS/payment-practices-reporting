package questionairre

sealed trait Decision

case class AskQuestion(question: Question) extends Decision

case class Exempt(reason: Option[String]) extends Decision

case object Required extends Decision