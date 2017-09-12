package controllers

import cats.data.Kleisli
import cats.instances.either._
import cats.syntax.either._
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import org.scalatestplus.play.PlaySpec
import webspec.WebSpec

trait QuestionnaireSteps {
  self: WebSpec with PlaySpec =>

  implicit class PageCallSyntax[T](k: PageCall[T]) {
    def withMessage(message: String): PageCall[T] =
      k andThen WithMessage(message)
  }

  def WithMessage(message: String): PageCall[HtmlPage] = Kleisli[ErrorOr, HtmlPage, HtmlPage] { page: HtmlPage =>
    page.paragraphText("reason").map { text =>
      text mustBe message
      page
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
