package calculator

import com.gargoylesoftware.htmlunit.html.{HtmlPage, HtmlSpan}
import forms.DateFields
import org.openqa.selenium.WebDriver
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerTest, PlaySpec}
import play.api.i18n.MessagesApi
import webspec.{SideStep, WebSpec}

import scala.language.postfixOps

class CalculatorSpec extends PlaySpec with WebSpec with GuiceOneServerPerSuite with OneBrowserPerTest with HtmlUnitFactory with TableDrivenPropertyChecks {

  override def createWebDriver(): WebDriver = HtmlUnitFactory.createWebDriver(false)

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  case class PeriodData(startDate: String, endDate: String, deadline: String)

  def Period(periodNum: Int, startDate: String, endDate: String, deadline: String): SideStep[HtmlPage, HtmlSpan] =
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

    forAll(periodTable) { (start, end, periods) =>
      val expectedPeriods: SideStep[HtmlPage, _] = periods.zipWithIndex.map { case (d, i) =>
        Period(i + 1, d.startDate, d.endDate, d.deadline)
      }.reduce((p1, p2) => p1 and p2)

      s"calculate periods and deadlines for start date $start and end date $end" in webSpec {
        OpenPage(CalculatorPage) andThen
          SetDateFields("startDate", start) andThen
          SetDateFields("endDate", end) andThen
          SubmitForm("Continue") should
          ShowPage(ReportingPeriodsAndDeadlinesPage) having {
          expectedPeriods
        }
      }
    }
  }

  lazy val periodTable = Table(
    ("Start date", "End date", "Expected periods"),
    (DateFields(1, 1, 2017), DateFields(31, 12, 2017), Seq(
      PeriodData("1 January 2018", "30 June 2018", "30 July 2018"),
      PeriodData("1 July 2018", "31 December 2018", "30 January 2019"))
    ),
    (DateFields(1, 1, 2016), DateFields(31, 12, 2016), Seq(
      PeriodData("1 January 2018", "30 June 2018", "30 July 2018"),
      PeriodData("1 July 2018", "31 December 2018", "30 January 2019"))
    ),
    (DateFields(1, 9, 2017), DateFields(31, 8, 2018), Seq(
      PeriodData("1 September 2017", "28 February 2018", "30 March 2018"),
      PeriodData("1 March 2018", "31 August 2018", "30 September 2018"))
    ),
    (DateFields(1, 1, 2018), DateFields(31, 3, 2019), Seq(
      PeriodData("1 January 2018", "30 June 2018", "30 July 2018"),
      PeriodData("1 July 2018", "31 March 2019", "30 April 2019"))
    ),
    (DateFields(1, 3, 2018), DateFields(28, 2, 2019), Seq(
      PeriodData("1 March 2018", "31 August 2018", "30 September 2018"),
      PeriodData("1 September 2018", "28 February 2019", "30 March 2019"))
    )
  )
}