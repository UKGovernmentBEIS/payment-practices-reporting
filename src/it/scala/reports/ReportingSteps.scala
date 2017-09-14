package reports

import cats.data.Kleisli
import cats.instances.either._
import cats.syntax.either._
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.{HtmlPage, HtmlTable, HtmlTableRow}
import controllers.ReportController
import org.scalatestplus.play.PlaySpec
import services.CompanySearchResult
import services.mocks.MockCompanySearch
import webspec.WebSpec

import scala.util.Try

trait ReportingSteps {
  self: WebSpec with PlaySpec =>

  val NavigateToSearchPage: PageStep[WebClient] = OpenPage(ReportingStartPage)
  val testCompany         : CompanySearchResult = MockCompanySearch.company1
  val testCompanyName     : String              = testCompany.companyName

  def StartPublishingForCompany(companyName: String): PageStep[WebClient] =
    NavigateToSearchPage andThen
      ClickButton(ReportController.searchButtonId) andThen
      ClickLink(companyName)

  def ChooseAndContinue(choice: String): PageStep[HtmlPage] =
    ChooseRadioButton(choice) andThen SubmitForm("Continue")

  def NavigateToReportingPeriodForm(companyName: String): PageStep[WebClient] = {
    StartPublishingForCompany(testCompanyName) andThen
      ClickLink("start-button") andThen
      ChooseAndContinue("account-yes") andThen
      SubmitForm("submit") andThen
      SubmitForm("submit")
  }

  def TableRowShouldHaveValue(rowName: String, value: String): PageStep[HtmlPage] = Kleisli[ErrorOr, HtmlPage, HtmlPage] { page: HtmlPage =>
    for {
      table <- page.findTable
      row <- table.getRowWithName(rowName)
      _ <- Try(row.getCell(1).getTextContent mustBe value).toErrorOr(s"Row '$rowName' has incorrect value")
    } yield page
  }

  //noinspection TypeAnnotation
  def ContainRow(rowName: String) = Kleisli[ErrorOr, HtmlTable, (HtmlTable, HtmlTableRow)] { table =>
    table.getRowWithName(rowName).map((table, _))
  }

  //noinspection TypeAnnotation
  def Value(v: String) = Kleisli[ErrorOr, HtmlTableRow, HtmlTableRow] { row =>
    val content = row.getCell(1).getTextContent
    if (content.trim === v) Right(row)
    else Left(SpecError(s"Expected text '$v' but found '$content'", None, None))
  }
}
