package controllers

import cats.instances.either._
import org.openqa.selenium.WebDriver
import org.scalatest.EitherValues
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerTest, PlaySpec}
import play.api.i18n.MessagesApi
import webspec.WebSpec

import scala.language.postfixOps

class QuestionnaireControllerSpec extends PlaySpec with WebSpec with GuiceOneServerPerSuite with OneBrowserPerTest with HtmlUnitFactory with EitherValues {

  override def createWebDriver(): WebDriver = HtmlUnitFactory.createWebDriver(false)

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  val webClient = new com.gargoylesoftware.htmlunit.WebClient()
  webClient.getOptions.setJavaScriptEnabled(false)

  private val NavigateToFirstQuestion =
    OpenPage(QuestionnaireStartPage) andThen ClickLink("Start now")

  private def ChooseAndContinue(choice: String) =
    ChooseRadioButton(choice) andThen SubmitForm("Continue")

  "questionnaire controller" should {
    "show start page" in {
      OpenPage(QuestionnaireStartPage) should
        ShowPage(QuestionnaireStartPage) run webClient
    }

    "should show first question" in {
      NavigateToFirstQuestion should
        ShowPage(CompanyOrLLPQuestionPage(messages)) run webClient
    }

    "should show 'No need to report' page if first question is answered 'No'" in {
      NavigateToFirstQuestion andThen
        ChooseAndContinue("no") should
        ShowPage(NoNeedToReportPage) run webClient
    }

    "should show 'No need to report' page in first year of operation" in {
      NavigateToFirstQuestion andThen
        ChooseAndContinue("yes") andThen
        ChooseAndContinue("first") should
        ShowPage(NoNeedToReportPage) run webClient
    }
  }
}