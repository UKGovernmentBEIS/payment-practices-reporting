package questionnaire

import controllers.QuestionnaireController
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
      val answers: Seq[YesNo] = Seq(a, b) ++ c.toList
      val path = answers.foldLeft(NavigateToSecondYear)((step, choice) => step andThen ChooseAndContinue(choice))

      s"not need to report if company answers are ${answers.mkString(", ")}" in webSpec {
        path should
          ShowPage(NoNeedToReportPage).withMessage("You should check at the beginning of every financial year to see if you need to report.")
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
      val answers: Seq[YesNo] = Seq(a, b) ++ c.toList
      val path = answers.foldLeft(NavigateToSecondYear)((step, choice) => step andThen ChooseAndContinue(choice))

      s"check subsidiaries if company answers are ${answers.mkString(", ")}" in webSpec {
        path should ShowPage(HasSubsidiariesPage)
      }
    }
  }

  "questionnaire controller in year 2 - no need to report after checking subsidiaries" should {
    val subsidiaryAnswers = Table(
      ("turnover", "balance", "employees"),
      (No, No, None),
      (No, Yes, Some(No)),
      (Yes, No, Some(No))
    )


    forAll(subsidiaryAnswers) { (a, b, c) =>
      val answers: Seq[YesNo] = Seq(a, b) ++ c.toList
      val path2 = answers.foldLeft(ChooseAndContinue(Yes))((step, choice) => step andThen ChooseAndContinue(choice))

      s"not need to report if subsidiary answers are ${answers.mkString(", ")}" in webSpec {
        NavigateToSubsidiaryQuestions andThen
          path2 should {
          ShowPage(NoNeedToReportPage) where {
            reason is "You should check at the beginning of every financial year to see if you need to report."
          }
        }
      }
    }
  }

  val companyData = Table(
    ("turnover", "balance", "employees", "reasons"),
    (Yes, Yes, None, Seq("company.turnover.y2", "company.balance.y2")),
    (Yes, No, Some(Yes), Seq("company.turnover.y2", "company.employees.y2")),
    (No, Yes, Some(Yes), Seq("company.balance.y2", "company.employees.y2"))
  )


  "questionnaire controller in year 2 - need to report, without checking subsidiaries" should {

    forAll(companyData) { (a, b, c, results) =>
      val answers: Seq[YesNo] = Seq(a, b) ++ c.toList
      val expectedReasons = results.map(key => messages(s"summary.$key"))
      val path = answers.foldLeft(NavigateToSecondYear)((step, choice) => step andThen ChooseAndContinue(choice))

      s"need to report when company answers are ${answers.mkString(", ")}" in webSpec {
        path andThen ChooseAndContinue(No) should {
          ShowPage(MustReportPage) where {
            List(QuestionnaireController.companyReasonsListId) should ContainItems(expectedReasons)
          }
        }
      }
    }
  }

  "questionnaire controller in year 2 - need to report after checking subsidiaries" should {
    val subsidiaryData = Table(
      ("turnover", "balance", "employees", "reasons"),
      (Yes, Yes, None, Seq("subsidiaries.turnover.y2", "subsidiaries.balance.y2")),
      (Yes, No, Some(Yes), Seq("subsidiaries.turnover.y2", "subsidiaries.employees.y2")),
      (No, Yes, Some(Yes), Seq("subsidiaries.balance.y2", "subsidiaries.employees.y2"))
    )

    forAll(companyData) { (a, b, c, companyReasons) =>
      val companyAnswers: Seq[YesNo] = Seq(a, b) ++ c.toList
      val expectedCompanyReasons = companyReasons.map(key => messages(s"summary.$key"))
      val companyPath = companyAnswers.foldLeft(NavigateToSecondYear)((step, choice) => step andThen ChooseAndContinue(choice))

      forAll(subsidiaryData) { (a, b, c, subsidiaryReasons) =>
        val subsidiaryAnswers: Seq[YesNo] = Seq(a, b) ++ c.toList
        val expectedSubsidiaryReasons = subsidiaryReasons.map(key => messages(s"summary.$key"))
        val subsidiaryPath = subsidiaryAnswers.foldLeft(ChooseAndContinue(Yes))((step, choice) => step andThen ChooseAndContinue(choice))

        s"need to report if company answers are ${companyAnswers.mkString(", ")} and subsidiary answers are ${subsidiaryAnswers.mkString(", ")}" in webSpec {
          companyPath andThen subsidiaryPath should {
            ShowPage(MustReportPage) where {
              (List(QuestionnaireController.companyReasonsListId) should ContainItems(expectedCompanyReasons)) and
                (List(QuestionnaireController.subsidiaryReasonsListId) should ContainItems(expectedSubsidiaryReasons))
            }
          }
        }
      }
    }
  }
}