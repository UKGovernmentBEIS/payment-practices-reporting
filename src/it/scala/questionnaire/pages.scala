package questionnaire

import controllers.{QuestionnaireController, routes}
import play.api.i18n.MessagesApi
import play.api.mvc.Call
import questionnaire.Questions.isCompanyOrLLPQuestion
import webspec.{EntryPoint, PageInfo}

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
