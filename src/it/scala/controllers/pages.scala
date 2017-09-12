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

  def call: Call

  /**
    * @return the title that appears on the page itself
    */
  def title: String

}

object QuestionnaireStartPageInfo extends PageInfo {
  val name : String = "Questionnaire Start Page"
  val call : Call   = routes.QuestionnaireController.start()
  val title: String = QuestionnaireController.startTitle
}

case class CompanyOrLLPQuestionPageInfo(messages: MessagesApi) extends PageInfo {
  override val name = "Company or LLP Question"

  override def call: Call = ???

  override val title = messages(isCompanyOrLLPQuestion.textKey)
}

object NoNeedToReportPageInfo extends PageInfo {
  override val name: String = "No need to report"

  override def call: Call = ???

  override val title: String = QuestionnaireController.exemptTitle
}
