package controllers

import org.jsoup.nodes.Document
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class QuestionnaireControllerSpec extends PlaySpec with GuiceOneServerPerSuite with ScalaFutures {

  import QuestionnaireController._
  import ResultDecoder._
  import questionnaire.Questions._

  implicit val client: WebClient = app.injector.instanceOf[WebClient]
  val messages = app.injector.instanceOf[MessagesApi]

  def navigateTo(page: Page): Future[Document] = client.get[Document](page.call)

  implicit class DocSyntax(doc: Document) {
    def clickOn(id: String): Future[Document] = Option(doc.getElementById(id)) match {
      case None                                    => fail(s"Could not find element with id '$id'")
      case Some(e) if e.tagName.toLowerCase == "a" => client.getUrl[Document](e.attr("href"))
    }
  }

  "questionnaire controller" should {
    "show start page" in {
      QuestionnaireStartPage.open.map { doc =>
        doc.title() mustBe startTitle
      }.futureValue(timeout(10 seconds))
    }

    "should show first question" in {
      for {
        startPage <- QuestionnaireStartPage.open
        doc <- startPage.clickOn(QuestionnaireController.startButtonId)
      } yield {
        doc.title() mustBe messages(isCompanyOrLLPQuestion.textKey)
      }
    }.futureValue(timeout(10 seconds))
  }
}
