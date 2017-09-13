package questionnaire

import cats.instances.either._
import org.openqa.selenium.WebDriver
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerTest, PlaySpec}
import play.api.i18n.MessagesApi
import utils.YesNo
import utils.YesNo.{No, Yes}
import webspec.WebSpec

import scala.language.postfixOps

class QuestionnaireYear2Spec extends PlaySpec with WebSpec with QuestionnaireSteps with GuiceOneServerPerSuite with OneBrowserPerTest with HtmlUnitFactory with TableDrivenPropertyChecks {

  override def createWebDriver(): WebDriver = HtmlUnitFactory.createWebDriver(false)

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  "questionnaire controller in year 2 - no need to check subsidiaries" should {
    val companyAnswers = Table(
      ("turnover", "balance", "employees"),
      (No, No, None),
      (No, Yes, Some(No)),
      (Yes, No, Some(No))
    )

    forAll(companyAnswers) { (a, b, c) =>
      val answers: Seq[YesNo] = Seq(Some(a), Some(b), c).flatten
      val path = answers.foldLeft(NavigateToSecondYear)((step, choice) => step andThen ChooseAndContinue(choice.entryName))

      s"not need to report if company answers are ${answers.mkString(", ")}" in webSpec {
        path should
          ShowPage(NoNeedToReportPage) withMessage "You should check at the beginning of every financial year to see if you need to report."
      }
    }
  }

  "questionnaire controller in year 2 - check subsidiaries" should {
    val companyAnswers = Table(
      ("turnover", "balance", "employees"),
      (Yes, Yes, None),
      (No, Yes, Some(Yes)),
      (Yes, No, Some(Yes))
    )

    forAll(companyAnswers) { (a, b, c) =>
      val answers: Seq[YesNo] = Seq(Some(a), Some(b), c).flatten
      val path = answers.foldLeft(NavigateToSecondYear)((step, choice) => step andThen ChooseAndContinue(choice.entryName))

      s"check subsidiaries if company answers are ${answers.mkString(", ")}" in webSpec {
        path should ShowPage(HasSubsidiariesPage)
      }
    }
  }

  "questionnaire controller in year 2 - no need to report after checking subsidiaries" should {
    val companyAnswers = Seq(Yes, Yes)
    val subsidiaryAnswers = Seq(
      Seq(No, No),
      Seq(No, Yes, No),
      Seq(Yes, No, No)
    )

    val navigateToSubsidiaryQuestions =
      companyAnswers.foldLeft(NavigateToSecondYear)((step, choice) => step andThen ChooseAndContinue(choice.entryName))

    subsidiaryAnswers.foreach { answers =>
      val path2 = answers.foldLeft(ChooseAndContinue("yes"))((step, choice) => step andThen ChooseAndContinue(choice.entryName))

      s"not need to report if subsidiary answers are ${answers.mkString(", ")}" in webSpec {
        navigateToSubsidiaryQuestions andThen
          path2 should
          ShowPage(NoNeedToReportPage) withMessage "You should check at the beginning of every financial year to see if you need to report."
      }
    }
  }
}