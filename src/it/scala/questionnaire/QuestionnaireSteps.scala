package questionnaire

import cats.data.Kleisli
import cats.instances.either._
import cats.syntax.either._
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import enumeratum.EnumEntry
import org.scalatestplus.play.PlaySpec
import questionnaire.FinancialYear.Second
import utils.YesNo.Yes
import webspec.WebSpec

import scala.util.Try

trait QuestionnaireSteps {
  self: WebSpec with PlaySpec =>

  implicit class ExtraQuestionnaireSyntax[T](k: PageStep[T]) {
    def withMessage(message: String): PageStep[T] =
      k andThen WithMessage(message)
  }

  def WithMessage(message: String): PageStep[HtmlPage] = Kleisli[ErrorOr, HtmlPage, HtmlPage] { page: HtmlPage =>
    page.paragraphText("reason").flatMap { text =>
      Try(text mustBe message).toErrorOr(s"Text of the 'reason' field did not match '$message'").map(_ => page)
    }
  }

  val NavigateToFirstQuestion: PageStep[WebClient] =
    OpenPage(QuestionnaireStartPage) andThen ClickLink("Start now")

  val NavigateToSecondYear: PageStep[WebClient] =
    NavigateToFirstQuestion andThen
      ChooseAndContinue(Yes) andThen
      ChooseAndContinue(Second)

  def ChooseAndContinue(choice: String): PageStep[HtmlPage] =
    ChooseRadioButton(choice) andThen SubmitForm("Continue")

  def ChooseAndContinue(choice: EnumEntry): PageStep[HtmlPage] =
    ChooseAndContinue(choice.entryName)
}
