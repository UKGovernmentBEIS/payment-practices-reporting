package questionairre

object Questions {
  val isCompanyOrLLCQuestion = YesNoQuestion()
  val financialYearQuestion = MultipleChoiceQuestion()
  val hasSubsidiariesQuestion = YesNoQuestion()

  val companyTurnoverQuestion = YesNoQuestion()
  val companyBalanceSheetQuestion = YesNoQuestion()
  val companyEmployeesQuestion = YesNoQuestion()
  val companyQuestionGroup = QuestionGroup(companyTurnoverQuestion, companyBalanceSheetQuestion, companyEmployeesQuestion)
  val subsidiaryTurnoverQuestion = YesNoQuestion()
  val subsidiaryBalanceSheetQuestion = YesNoQuestion()
  val subsidiaryEmployeesQuestion = YesNoQuestion()
  val subsidiariesQuestionGroup = QuestionGroup(subsidiaryTurnoverQuestion, subsidiaryBalanceSheetQuestion, subsidiaryEmployeesQuestion)
}
