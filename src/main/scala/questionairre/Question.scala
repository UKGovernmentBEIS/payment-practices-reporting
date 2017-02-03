package questionairre

sealed trait Question

case class YesNoQuestion() extends Question

case class MultipleChoiceQuestion() extends Question

case class QuestionGroup(turnoverQuestion: Question, balanceSheetQuestion: Question, employeesQuestion: Question)