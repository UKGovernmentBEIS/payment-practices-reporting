package questionnaire

import org.openqa.selenium.WebDriver
import cats.instances.either._
import cats.syntax.either._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerTest, PlaySpec}
import play.api.i18n.MessagesApi
import utils.YesNo.{No, Yes}
import webspec.WebSpec

import scala.language.postfixOps

class QuestionnaireYear2Spec extends PlaySpec with WebSpec with QuestionnaireSteps with GuiceOneServerPerSuite with OneBrowserPerTest with HtmlUnitFactory {

  override def createWebDriver(): WebDriver = HtmlUnitFactory.createWebDriver(false)

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  "questionnaire controller in year 2 - no need to check subsidiaries" should {
    val companyAnswers = Seq(
      Seq(No, No),
      Seq(No, Yes, No),
      Seq(Yes, No, No)
    )
    companyAnswers.foreach { answers =>
      val path = answers.foldLeft(NavigateToSecondYear)((step, choice) => step andThen ChooseAndContinue(choice.entryName))

      s"not need to report if company answers are ${answers.mkString(", ")}" in webSpec {
        path should
          ShowPage(NoNeedToReportPage) withMessage "You should check at the beginning of every financial year to see if you need to report."
      }
    }
  }

  "questionnaire controller in year 2 - check subsidiaries" should {
    val companyAnswers = Seq(
      Seq(Yes, Yes),
      Seq(No, Yes, Yes),
      Seq(Yes, No, Yes)
    )
    companyAnswers.foreach { answers =>
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