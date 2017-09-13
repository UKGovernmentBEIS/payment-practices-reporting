package questionnaire

import cats.data.Kleisli
import cats.instances.either._
import cats.syntax.either._
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import org.scalatestplus.play.PlaySpec
import webspec.WebSpec

import scala.util.Try

trait QuestionnaireSteps {
  self: WebSpec with PlaySpec =>

  implicit class PageCallSyntax[T](k: PageCall[T]) {
    def withMessage(message: String): PageCall[T] =
      k andThen WithMessage(message)
  }

  def WithMessage(message: String): PageCall[HtmlPage] = Kleisli[ErrorOr, HtmlPage, HtmlPage] { page: HtmlPage =>
    page.paragraphText("reason").flatMap { text =>
      Try(text mustBe message).toErrorOr(s"Text of the 'reason' field did not match '$message'").map(_ => page)
    }
  }

  val NavigateToFirstQuestion: PageCall[WebClient] =
    OpenPage(QuestionnaireStartPage) andThen ClickLink("Start now")

  val NavigateToSecondYear: PageCall[WebClient] =
    NavigateToFirstQuestion andThen
      ChooseAndContinue("yes") andThen
      ChooseAndContinue("second")

  def ChooseAndContinue(choice: String): PageCall[HtmlPage] =
    ChooseRadioButton(choice) andThen SubmitForm("Continue")
}
