package questionnaire

import cats.syntax.either._
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.{HtmlPage, HtmlParagraph}
import enumeratum.EnumEntry
import org.scalatestplus.play.PlaySpec
import questionnaire.FinancialYear._
import utils.YesNo.Yes
import webspec._

import scala.util.Try

trait QuestionnaireSteps {
  self: WebSpec with PlaySpec =>

  implicit class ExtraQuestionnaireSyntax[T](s: PageStep) {
    def withMessage(message: String): PageStep =
      s andThen WithMessage(message)
  }

  def WithMessage(message: String): PageStep = Step { page: HtmlPage =>
    page.paragraphText("reason").flatMap { text =>
      Try(text mustBe message).toErrorOr(s"Text of the 'reason' field did not match '$message'").map(_ => page)
    }
  }

  val NavigateToFirstQuestion: Scenario[HtmlPage] =
    OpenPage(QuestionnaireStartPage) andThen ClickLink("Start now")

  val NavigateToSecondYear: Scenario[HtmlPage] =
    NavigateToFirstQuestion andThen
      ChooseAndContinue(Yes) andThen
      ChooseAndContinue(Second)

  val NavigateToThirdYear: Scenario[HtmlPage] =
    NavigateToFirstQuestion andThen
      ChooseAndContinue(Yes) andThen
      ChooseAndContinue(ThirdOrLater)

  def ChooseAndContinue(choice: String): PageStep =
    ChooseRadioButton(choice) andThen SubmitForm("Continue")

  def ChooseAndContinue(choice: EnumEntry): PageStep =
    ChooseAndContinue(choice.entryName)

  val reason: OptionalSideStep[HtmlPage, HtmlParagraph] = Element[HtmlParagraph]("reason")

  val NavigateToSubsidiaryQuestions: Scenario[HtmlPage] =
    NavigateToSecondYear andThen
      ChooseAndContinue(Yes) andThen
      ChooseAndContinue(Yes)
}
