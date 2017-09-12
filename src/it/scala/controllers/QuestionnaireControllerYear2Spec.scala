package controllers

import cats.instances.either._
import org.openqa.selenium.WebDriver
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerTest, PlaySpec}
import play.api.i18n.MessagesApi
import webspec.WebSpec

import scala.language.postfixOps

class QuestionnaireControllerYear2Spec extends PlaySpec with WebSpec with QuestionnaireSteps with GuiceOneServerPerSuite with OneBrowserPerTest with HtmlUnitFactory {

  override def createWebDriver(): WebDriver = HtmlUnitFactory.createWebDriver(false)

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  "questionnaire controller in year 2" should {
    "No need to report without subsidiaries" in webSpec {
      NavigateToSecondYear andThen
        ChooseAndContinue("no") andThen
        ChooseAndContinue("no") should
        ShowPage(NoNeedToReportPage) withMessage "You should check at the beginning of every financial year to see if you need to report."
    }
  }
}