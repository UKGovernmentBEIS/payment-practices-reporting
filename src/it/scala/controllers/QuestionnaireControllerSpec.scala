package controllers

import cats.instances.either._
import org.openqa.selenium.WebDriver
import org.scalatest.EitherValues
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerTest, PlaySpec}
import play.api.i18n.MessagesApi

import scala.language.postfixOps

class QuestionnaireControllerSpec extends PlaySpec with GuiceOneServerPerSuite with OneBrowserPerTest with HtmlUnitFactory with EitherValues {

  override def createWebDriver(): WebDriver = HtmlUnitFactory.createWebDriver(false)

  import QuestionnaireController._

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  val webClient = new com.gargoylesoftware.htmlunit.WebClient()
  webClient.getOptions.setJavaScriptEnabled(false)

  import support.syntax._

  private val NavigateToFirstQuestion =
    ShowPage(QuestionnaireStartPageInfo) andThen ClickLink("Start now")

  private def ChooseAndContinue(choice: String) =
    ChooseRadioButton(choice) andThen SubmitForm("Continue")

  "questionnaire controller" should {
    "show start page" in {
      val result =
        ShowPage(QuestionnaireStartPageInfo) run webClient

      result mustBe a[Right[_, _]]
      eventually(result.right.value.getTitleText mustBe startTitle)
    }

    "should show first question" in {
      val result =
        NavigateToFirstQuestion run webClient

      result mustBe a[Right[_, _]]
      eventually(result.right.value.getTitleText mustBe CompanyOrLLPQuestionPageInfo(messages).title)
    }

    "should show 'No need to report' page if first question is answered 'No'" in {
      val result =
        NavigateToFirstQuestion andThen
          ChooseAndContinue("no") run webClient

      result mustBe a[Right[_, _]]
      eventually(result.right.value.getTitleText mustBe QuestionnaireController.exemptTitle)
    }

    "should show exempt in first year of operation" in {
      val result =
        NavigateToFirstQuestion andThen
          ChooseAndContinue("yes") andThen
          ChooseAndContinue("first") run webClient

      result mustBe a[Right[_, _]]
      eventually(result.right.value.getTitleText mustBe QuestionnaireController.exemptTitle)
    }
  }
}