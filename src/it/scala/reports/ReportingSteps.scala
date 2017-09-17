package reports

import cats.syntax.either._
import com.gargoylesoftware.htmlunit.html.{HtmlPage, HtmlTable, HtmlTableRow}
import controllers.ReportController
import org.scalatestplus.play.PlaySpec
import services.CompanySearchResult
import services.mocks.MockCompanySearch
import webspec._

import scala.util.Try

trait ReportingSteps {
  self: WebSpec with PlaySpec =>

  val NavigateToSearchPage: Scenario[HtmlPage]  = OpenPage(ReportingStartPage)
  val testCompany         : CompanySearchResult = MockCompanySearch.company1
  val testCompanyName     : String              = testCompany.companyName

  def StartPublishingForCompany(companyName: String): Scenario[HtmlPage] =
    NavigateToSearchPage andThen
      ClickButton(ReportController.searchButtonId) andThen
      ClickLink(companyName)

  def ChooseAndContinue(choice: String): PageStep =
    ChooseRadioButton(choice) andThen SubmitForm("Continue")

  def NavigateToReportingPeriodForm(companyName: String): Scenario[HtmlPage] = {
    StartPublishingForCompany(testCompanyName) andThen
      ClickLink("start-button") andThen
      ChooseAndContinue("account-yes") andThen
      SubmitForm("submit") andThen
      SubmitForm("submit")
  }

  def TableRowShouldHaveValue(rowName: String, value: String): PageStep = Step { page: HtmlPage =>
    for {
      table <- page.findTable
      row <- table.findRowWithName(rowName)
      _ <- Try(row.getCell(1).getTextContent mustBe value).toErrorOr(s"Row '$rowName' has incorrect value")
    } yield page
  }

  //noinspection TypeAnnotation
  def ContainRow(rowName: String) = SideStep[HtmlTable, HtmlTableRow] { table: HtmlTable =>
    table.findRowWithName(rowName).map((table, _))
  }

  //noinspection TypeAnnotation
  def Value(v: String) = Step[HtmlTableRow, HtmlTableRow] { row: HtmlTableRow =>
    val content = row.getCell(1).getTextContent
    if (content.trim === v) Right(row)
    else Left(SpecError(s"Expected text '$v' but found '$content'", None, None))
  }
}
