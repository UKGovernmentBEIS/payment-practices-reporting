package controllers

import cats.data.OptionT
import cats.instances.future._
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

  implicit val client: WebClient = app.injector.instanceOf[WebClient]
  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  def navigateTo(page: PageInfo): Future[Document] = client.get[Document](page.call)

  implicit class DocSyntax(doc: Document) {
    def clickLink(id: String): Future[Document] = Option(doc.getElementById(id)) match {
      case None                                     => fail(s"Could not find element with id '$id'")
      case Some(e) if e.tagName.toLowerCase === "a" => client.getUrl[Document](e.attr("href"))
    }
  }

  "questionnaire controller" should {
    "show start page" in {
      QuestionnaireStartPageInfo.open.map { doc =>
        doc.title() mustBe startTitle
      }.futureValue(timeout(10 seconds))
    }

    "should show first question" in {
      for {
        startPage <- QuestionnaireStartPageInfo.open
        page <- startPage.clickLink(startButtonId).map(CompanyOrLLPQuestionPage(_, messages))
      } yield {
        page.title mustBe CompanyOrLLPQuestionPageInfo(messages).title
      }
    }.futureValue(timeout(10 seconds))

    "should show 'No need to report' page if first question is answered 'No'" in {
      for {
        startPage <- OptionT.liftF(QuestionnaireStartPageInfo.open)
        q1Page <- OptionT.liftF(startPage.clickLink(startButtonId).map(CompanyOrLLPQuestionPage(_, messages)))
        form <- OptionT.fromOption(q1Page.form("question-form"))
        noNeed <- OptionT.liftF(form.selectRadio("no").submit.map(NoNeedToReportPage))
      } yield {
        noNeed.title mustBe NoNeedToReportPageInfo.title
      }
    }.value.futureValue(timeout(10 seconds))
  }
}
