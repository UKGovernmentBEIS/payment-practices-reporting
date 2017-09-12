package controllers

import cats.instances.either._
import org.openqa.selenium.WebDriver
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerTest, PlaySpec}
import play.api.i18n.MessagesApi
import webspec.WebSpec

import scala.language.postfixOps

class QuestionnaireControllerSpec extends PlaySpec with WebSpec with QuestionnaireSteps with GuiceOneServerPerSuite with OneBrowserPerTest with HtmlUnitFactory {

  override def createWebDriver(): WebDriver = HtmlUnitFactory.createWebDriver(false)

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  "questionnaire controller" should {
    "show start page" in webSpec {
      OpenPage(QuestionnaireStartPage) should
        ShowPage(QuestionnaireStartPage)
    }

    "should show first question" in webSpec {
      NavigateToFirstQuestion should
        ShowPage(CompanyOrLLPQuestionPage(messages))
    }

    "should show 'No need to report' page if first question is answered 'No'" in webSpec {
      NavigateToFirstQuestion andThen
        ChooseAndContinue("no") should
        ShowPage(NoNeedToReportPage)
    }

    "should show 'No need to report' page in first year of operation" in webSpec {
      NavigateToFirstQuestion andThen
        ChooseAndContinue("yes") andThen
        ChooseAndContinue("first") should
        ShowPage(NoNeedToReportPage)
    }
  }
}