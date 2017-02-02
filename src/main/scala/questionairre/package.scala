package object questionairre {

  case class Question(number: Int, text: String, hint: Option[String], choices: List[String], inline: Boolean)

  sealed trait Answer

  case object No extends Answer

  case object Yes extends Answer

  sealed trait Decision

  case object NeedToFile extends Decision

  case object NoNeedToFile extends Decision

  case object DontKnowYet extends Decision


  def doINeedToFile(answers: Set[Answer]): Decision = ???

}
