package controllers

import org.jsoup.nodes.Document
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Call

import scala.concurrent.Future

trait Page {
  def name: String
  def call: Call
  def title: String

  def open(implicit client: WebClient): Future[Document] = client.get[Document](call)
}

object QuestionnaireStartPage extends Page {
  val name : String = "Questionnaire Start Page"
  val call : Call   = routes.QuestionnaireController.start()
  val title: String = QuestionnaireController.startTitle
}

object NoNeedToReport extends Page  {
  override def name: String = "No need to report"

  override def call: Call = ???

  override def title: String = "Your business does not need to publish reports"
}