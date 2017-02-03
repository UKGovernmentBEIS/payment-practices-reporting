package questionairre

import questionairre.Answer._

case class DecisionState(
                          isCompanyOrLLP: Answer,
                          financialYear: FinancialYear,
                          companyAnswers: AnswerGroup,
                          subsidiaries: Answer,
                          subsidiaryAnswers: AnswerGroup
                        )

object Decider {

  import FinancialYear._
  import Questions._

  def calculateDecision(state: DecisionState): Decision = state.isCompanyOrLLP match {
    case Unanswered => AskQuestion(isCompanyOrLLCQuestion)
    case No => Exempt(None)
    case Yes => checkFinancialYear(state)
  }

  def checkFinancialYear(state: DecisionState): Decision = state.financialYear match {
    case Unknown => AskQuestion(financialYearQuestion)
    case First => Exempt(Some("reason.firstyear"))
    case Second | ThirdOrLater => checkCompanyAnswers(state)
  }

  def checkCompanyAnswers(state: DecisionState): Decision = state.companyAnswers.nextQuestion(companyQuestionGroup) match {
    case Some(question) => AskQuestion(question)
    case None if state.companyAnswers.score >= 2 => checkIfSubsidiaries(state)
    case None => Exempt(Some("reason.company.notlargeenough"))
  }

  def checkIfSubsidiaries(state: DecisionState): Decision = state.subsidiaries match {
    case Unanswered => AskQuestion(hasSubsidiariesQuestion)
    case No => Required
    case Yes => checkSubsidiaryAnswers(state)
  }

  def checkSubsidiaryAnswers(state: DecisionState): Decision = state.subsidiaryAnswers.nextQuestion(companyQuestionGroup) match {
    case Some(question) => AskQuestion(question)
    case None if state.companyAnswers.score >= 2 => Required
    case None => Exempt(Some("reason.group.notlargeenough"))
  }
}