package calculator

import com.gargoylesoftware.htmlunit.html.HtmlSpan
import forms.DateFields
import org.openqa.selenium.WebDriver
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerTest, PlaySpec}
import play.api.i18n.MessagesApi
import webspec.WebSpec

import scala.language.postfixOps

class CalculatorErrorsSpec extends PlaySpec with WebSpec with GuiceOneServerPerSuite with OneBrowserPerTest with HtmlUnitFactory with TableDrivenPropertyChecks {

  override def createWebDriver(): WebDriver = HtmlUnitFactory.createWebDriver(false)

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  "calculator" should {
    val Continue = SubmitForm("Continue")
    "show errors when no data is entered" in webSpec {
      OpenPage(CalculatorPage) andThen Continue should
        ShowPageWithErrors(CalculatorPage) where {
        Element[HtmlSpan]("form-errors") is "These dates are not valid" and
          (Element[HtmlSpan]("error-startDate") is "This date is not valid") and
          (Element[HtmlSpan]("error-endDate") is "This date is not valid")
      }
    }

    "show an error when the end date is before the start date" in webSpec {
      OpenPage(CalculatorPage) andThen
        SetDateFields("startDate", DateFields(1, 1, 2018)) andThen
        SetDateFields("endDate", DateFields(1, 11, 2017)) andThen
        Continue should
        ShowPageWithErrors(CalculatorPage) where {
        Element[HtmlSpan]("form-errors") is "The end date must be later than the start date"
      }
    }

    "show an error when the start date is invalid" in webSpec {
      OpenPage(CalculatorPage) andThen
        SetDateFields("startDate", DateFields(31, 2, 2017)) andThen
        Continue should
        ShowPageWithErrors(CalculatorPage) where {
        Element[HtmlSpan]("form-errors") is "These dates are not valid" and
          (Element[HtmlSpan]("error-startDate") is "This date is not valid")
      }
    }
    "show an error when the end date is invalid" in webSpec {
      OpenPage(CalculatorPage) andThen
        SetDateFields("endDate", DateFields(31, 2, 2017)) andThen
        Continue should
        ShowPageWithErrors(CalculatorPage) where {
        Element[HtmlSpan]("form-errors") is "These dates are not valid" and
          (Element[HtmlSpan]("error-endDate") is "This date is not valid")
      }
    }
  }
}