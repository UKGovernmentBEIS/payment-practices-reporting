package support

import cats.data.Kleisli
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.{HtmlElement, HtmlPage, HtmlRadioButtonInput, HtmlSubmitInput}
import controllers.PageInfo
import play.api.mvc.Call
import play.api.test.Helpers

import scala.util.{Failure, Success, Try}

object syntax {
  lazy val baseUrl =
    s"http://localhost:${Helpers.testServerPort}"

  def url(s: String): String = baseUrl + s

  def url(call: Call): String = baseUrl + call.url

  type ErrorOr[T] = Either[String, T]

  implicit class TrySyntax[T](t: Try[T]) {
    def toEither(prefix: String): ErrorOr[T] = t match {
      case Success(v)         => Right(v)
      case Failure(throwable) => Left(s"$prefix: ${throwable.getMessage}")
    }
  }

  implicit class WebClientSyntax(webClient: WebClient) {
    def show(call: Call): ErrorOr[HtmlPage] =
      Try(webClient.getPage[HtmlPage](url(call))).toEither("show")
  }

  implicit class PageSyntax(page: HtmlPage) {
    def byId[T <: HtmlElement](id: String): ErrorOr[T] =
      Try(page.getHtmlElementById[T](id)).toEither("byId")

    def clickLink(name: String): ErrorOr[HtmlPage] =
      Try(page.getAnchorByName(name).click[HtmlPage]()).toEither("clickLink")

    def chooseRadioButton(id: String): ErrorOr[HtmlPage] = {
      for {
        radio <- Try(page.getHtmlElementById[HtmlRadioButtonInput](id))
        _ = radio.setChecked(true)
      } yield page
    }.toEither("chooseRadioButton")

    def submitForm(buttonName: String): ErrorOr[HtmlPage] = {
      for {
        submit <- Try(page.getElementByName[HtmlSubmitInput](buttonName))
        page = submit.click[HtmlPage]()
      } yield page
    }.toEither("submitForm")
  }

  type PageCall[T] = Kleisli[ErrorOr, T, HtmlPage]

  def ShowPage(pageInfo: PageInfo): PageCall[WebClient] = Kleisli((webClient:WebClient) => webClient.show(pageInfo.call))

  def ClickLink(name: String): PageCall[HtmlPage] = Kleisli((page: HtmlPage) => page.clickLink(name))

  def ChooseRadioButton(id: String): PageCall[HtmlPage] = Kleisli((page: HtmlPage) => page.chooseRadioButton(id))

  def SubmitForm(buttonName: String): PageCall[HtmlPage] = Kleisli((page: HtmlPage) => page.submitForm(buttonName))
}
