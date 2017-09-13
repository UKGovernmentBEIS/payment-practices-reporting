package reports

import cats.instances.either._
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.{HtmlForm, HtmlPage, HtmlParagraph}
import controllers.{ReportController, ReportingPeriodController, ShortFormController}
import org.openqa.selenium.WebDriver
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerTest, PlaySpec}
import play.api.i18n.MessagesApi
import services.mocks.MockCompanySearch
import webspec.WebSpec

import scala.language.postfixOps

class ShortReportSpec extends PlaySpec with WebSpec with GuiceOneServerPerSuite with OneBrowserPerTest with HtmlUnitFactory {

  override def createWebDriver(): WebDriver = HtmlUnitFactory.createWebDriver(false)

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  private val NavigateToSearchPage = OpenPage(ReportingStartPage)

  private val testCompany     = MockCompanySearch.companies.head
  private val testCompanyName = testCompany.companyName

  def StartPublishingForCompany(companyName: String): PageCall[WebClient] =
    NavigateToSearchPage andThen
      ClickButton(ReportController.searchButtonId) andThen
      ClickLink(companyName)

  def ChooseAndContinue(choice: String): PageCall[HtmlPage] =
    ChooseRadioButton(choice) andThen SubmitForm("Continue")

  private def NavigateToReportingPeriodForm(companyName: String): PageCall[WebClient] = {
    StartPublishingForCompany(testCompanyName) andThen
      ClickLink("start-button") andThen
      ChooseAndContinue("account-yes") andThen
      SubmitForm("submit") andThen
      SubmitForm("submit")
  }

  "the search page" should {
    "let me navigate to publishing start page" in webSpec {
      StartPublishingForCompany(testCompanyName) should
        ShowPage(PublishFor(testCompanyName))
          .containingElement[HtmlParagraph](ReportController.companyNumberParagraphId)(_.getTextContent.contains(testCompany.companiesHouseId.id))
    }
  }

  "selecting a company and navigating through the sign-in" should {
    "show reporting period form" in webSpec {
      NavigateToReportingPeriodForm(testCompanyName) should
        ShowPage(PublishFor(testCompanyName)) withElementById[HtmlForm] ReportingPeriodController.reportingPeriodFormId
    }
  }

  "entering valid dates and selecting No Qualifying Contracts" should {
    "show the short form" in webSpec {
      NavigateToReportingPeriodForm(testCompanyName) andThen
        SetDateFields("reportDates.startDate", 1, 5, 2017) andThen
        SetDateFields("reportDates.endDate", 1, 6, 2017) andThen
        ChooseAndContinue("hasQualifyingContracts-no") should
        ShowPage(ShortFormPage(testCompany)) withElementById[HtmlForm] ShortFormController.shortFormId
    }
  }


}