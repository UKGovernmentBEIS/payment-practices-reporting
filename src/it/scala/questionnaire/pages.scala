package questionnaire

import controllers.{QuestionnaireController, routes}
import play.api.i18n.MessagesApi
import play.api.mvc.Call
import questionnaire.Questions.isCompanyOrLLPQuestion
import webspec.{EntryPoint, PageInfo}

object QuestionnaireStartPage extends PageInfo with EntryPoint {
  override val title: String = QuestionnaireController.startTitle
  override val call : Call   = routes.QuestionnaireController.start()
}

case class CompanyOrLLPQuestionPage(messages: MessagesApi) extends PageInfo {
  override val title = messages(isCompanyOrLLPQuestion.textKey)
}

object NoNeedToReportPage extends PageInfo {
  override val title: String = QuestionnaireController.exemptTitle
}

object MustReportPage extends PageInfo {
  override val title: String = QuestionnaireController.mustReportTitle
}

object HasSubsidiariesPage extends PageInfo {
  override val title: String = "Does your business have subsidiaries?"
}
