package reports

import com.gargoylesoftware.htmlunit.html.{HtmlPage, HtmlParagraph}
import controllers.{ReportController, ReportingPeriodController, ReviewPage, ShortFormController}
import forms.DateFields
import org.openqa.selenium.WebDriver
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerTest, PlaySpec}
import play.api.i18n.MessagesApi
import webspec.{Scenario, WebSpec}

import scala.language.postfixOps

class ShortReportSpec extends PlaySpec with WebSpec with GuiceOneServerPerSuite with OneBrowserPerTest with HtmlUnitFactory with ReportingSteps {

  override def createWebDriver(): WebDriver = HtmlUnitFactory.createWebDriver(false)

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  private def NavigateToShortForm(companyName: String, startDate: DateFields = DateFields(1, 5, 2017), endDate: DateFields = DateFields(1, 6, 2017)): Scenario[HtmlPage] = {
    NavigateToReportingPeriodForm(testCompanyName) andThen
      SetDateFields("reportDates.startDate", startDate) andThen
      SetDateFields("reportDates.endDate", endDate) andThen
      ChooseAndContinue("hasQualifyingContracts-no")
  }

  "the search page" should {
    "let me navigate to publishing start page" in webSpec {
      StartPublishingForCompany(testCompanyName) should {
        ShowPage(PublishFor(testCompanyName)) where {
          Element[HtmlParagraph](ReportController.companyNumberParagraphId) should {
            ContainText[HtmlParagraph](testCompany.companiesHouseId.id)
          }
        }
      }
    }
  }

  "selecting a company and navigating through the sign-in" should {
    "show reporting period form" in webSpec {
      NavigateToReportingPeriodForm(testCompanyName) should {
        ShowPage(PublishFor(testCompanyName)) where {
          Form(ReportingPeriodController.reportingPeriodFormId) exists
        }
      }
    }
  }

  "entering valid dates and selecting No Qualifying Contracts" should {
    "show the short form" in webSpec {
      NavigateToShortForm(testCompanyName) should {
        ShowPage(ShortFormPage(testCompany)) where {
          Form(ShortFormController.shortFormId) exists
        }
      }
    }
  }

  "selecting no payment codes and submitting" should {
    "show the review page" in webSpec {
      NavigateToShortForm(testCompanyName) andThen
        ChooseAndContinue("paymentCodes.yesNo-no") should {
        ShowPage(ShortReviewPage) where {
          Table(ReviewPage.reviewTableId) should {
            ContainRow("Start date of reporting period") having Value("1 May 2017") and
              ContainRow("End date of reporting period") having Value("1 June 2017") and
              ContainRow("Are you a member of a code of conduct or standards on payment practices?") having Value("No")
          }
        }
      }
    }
  }

  "entering a value for payment code and submitting" should {
    "show the review page" in webSpec {
      NavigateToShortForm(testCompanyName) andThen
        ChooseRadioButton("paymentCodes.yesNo-yes") andThen
        SetTextField("paymentCodes.text", "payment codes") andThen
        SubmitForm("Continue") should {
        ShowPage(ShortReviewPage) where {
          Table(ReviewPage.reviewTableId) should {
            ContainRow("Start date of reporting period") having Value("1 May 2017") and
              ContainRow("End date of reporting period") having Value("1 June 2017") and
              ContainRow("Are you a member of a code of conduct or standards on payment practices?") having Value("Yes – payment codes")
          }
        }
      }
    }
  }
}