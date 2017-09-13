package reports

import cats.instances.either._
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import controllers.ReportController
import org.scalatestplus.play.PlaySpec
import services.CompanySearchResult
import services.mocks.MockCompanySearch
import webspec.WebSpec

trait ReportingSteps {
  self: WebSpec with PlaySpec =>

  val NavigateToSearchPage: PageCall[WebClient] = OpenPage(ReportingStartPage)
  val testCompany         : CompanySearchResult = MockCompanySearch.companies.head
  val testCompanyName     : String              = testCompany.companyName

  def StartPublishingForCompany(companyName: String): PageCall[WebClient] =
    NavigateToSearchPage andThen
      ClickButton(ReportController.searchButtonId) andThen
      ClickLink(companyName)

  def ChooseAndContinue(choice: String): PageCall[HtmlPage] =
    ChooseRadioButton(choice) andThen SubmitForm("Continue")

  def NavigateToReportingPeriodForm(companyName: String): PageCall[WebClient] = {
    StartPublishingForCompany(testCompanyName) andThen
      ClickLink("start-button") andThen
      ChooseAndContinue("account-yes") andThen
      SubmitForm("submit") andThen
      SubmitForm("submit")
  }


}
