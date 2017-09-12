package controllers

import play.api.i18n.MessagesApi
import play.api.mvc.Call
import questionnaire.Questions.isCompanyOrLLPQuestion

trait PageInfo {

  /**
    * @return an name that identifies the page in testing. It does not need to
    *         correspond to any name or text for the page itself.
    */
  def name: String

  /**
    * @return the title that appears on the page itself
    */
  def title: String
}

trait EntryPoint {
  def call: Call
}

object QuestionnaireStartPage extends PageInfo with EntryPoint {
  val name : String = "Questionnaire Start Page"
  val title: String = QuestionnaireController.startTitle
  val call : Call   = routes.QuestionnaireController.start()
}

case class CompanyOrLLPQuestionPage(messages: MessagesApi) extends PageInfo {
  override val name = "Company or LLP Question"
  override val title = messages(isCompanyOrLLPQuestion.textKey)
}

object NoNeedToReportPage extends PageInfo {
  override val name: String = "No need to report"
  override val title: String = QuestionnaireController.exemptTitle
}

object HasSubsidiariesPage extends PageInfo {
  override val name: String = "Has Subsidiaries"
  override val title: String = "Does your business have subsidiaries?"
}
