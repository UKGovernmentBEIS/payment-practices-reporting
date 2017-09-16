package calculator

import com.gargoylesoftware.htmlunit.html.{HtmlPage, HtmlSpan}
import forms.DateFields
import org.openqa.selenium.WebDriver
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerTest, PlaySpec}
import play.api.i18n.MessagesApi
import webspec.{SideStep, WebSpec}

import scala.language.postfixOps

class CalculatorSpec extends PlaySpec with WebSpec with GuiceOneServerPerSuite with OneBrowserPerTest with HtmlUnitFactory {

  override def createWebDriver(): WebDriver = HtmlUnitFactory.createWebDriver(false)

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  def Period(periodNum: Int, startDate: String, endDate: String, deadline: String): SideStep[HtmlPage, _] =
    (Element[HtmlSpan](s"period-start-$periodNum") is startDate) and
      (Element[HtmlSpan](s"period-end-$periodNum") is endDate) and
      (Element[HtmlSpan](s"deadline-$periodNum") is deadline)

  "calculator" should {
    "calculate one period and deadline" in webSpec {
      OpenPage(CalculatorPage) andThen
        SetDateFields("startDate", DateFields(1, 1, 2018)) andThen
        SetDateFields("endDate", DateFields(30, 9, 2018)) andThen
        SubmitForm("Continue") should
        ShowPage(ReportingPeriodsAndDeadlinesPage) having
        Period(1, "1 January 2018", "30 September 2018", "30 October 2018")
    }
  }
}