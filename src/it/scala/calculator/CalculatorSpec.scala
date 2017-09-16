package calculator

import com.gargoylesoftware.htmlunit.html.HtmlSpan
import forms.DateFields
import org.openqa.selenium.WebDriver
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerTest, PlaySpec}
import play.api.i18n.MessagesApi
import webspec.WebSpec

import scala.language.postfixOps

class CalculatorSpec extends PlaySpec with WebSpec with GuiceOneServerPerSuite with OneBrowserPerTest with HtmlUnitFactory {

  override def createWebDriver(): WebDriver = HtmlUnitFactory.createWebDriver(false)

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  "calculator" should {
    "calculate one period and deadline" in webSpec {
      OpenPage(CalculatorPage) andThen
        SetDateFields("startDate", DateFields(1, 1, 2018)) andThen
        SetDateFields("endDate", DateFields(30, 9, 2018)) andThen
        SubmitForm("Continue") should {
        ShowPage(ReportingPeriodsAndDeadlinesPage) where {
          (Element[HtmlSpan]("period-start-1") is "1 January 2018") and
            (Element[HtmlSpan]("period-end-1") is "30 September 2018") and
            (Element[HtmlSpan]("deadline-1") is "30 October 2018")
        }
      }
    }
  }
}