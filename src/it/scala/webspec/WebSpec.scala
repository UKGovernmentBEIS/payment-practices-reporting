package webspec

import cats.data.Kleisli
import cats.syntax.either._
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html._
import org.scalatest.EitherValues
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.{OneBrowserPerTest, PlaySpec}
import play.api.mvc.Call
import play.api.test.Helpers

import scala.util.{Failure, Success, Try}

trait WebSpec extends EitherValues {
  self: PlaySpec with Eventually with OneBrowserPerTest =>

  implicit val webClient: WebClient = new com.gargoylesoftware.htmlunit.WebClient()
  webClient.getOptions.setJavaScriptEnabled(false)

  lazy val baseUrl =
    s"http://localhost:${Helpers.testServerPort}"

  def url(s: String): String = baseUrl + s

  def url(call: Call): String = baseUrl + call.url

  type ErrorOr[T] = Either[String, T]

  def webSpec(spec: PageCall[WebClient])(implicit wc: WebClient): ErrorOr[HtmlPage] =
    spec run wc

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

    def clickButton(id: String): ErrorOr[HtmlPage] =
      Try(page.getHtmlElementById[HtmlButton](id).click[HtmlPage]()).toEither("clickButton")

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

    def paragraphText(id: String): ErrorOr[String] = {
      byId[HtmlParagraph](id).map(_.getTextContent)
    }
  }

  implicit class ExtraKleisliSyntax[F[_], A, B](k: Kleisli[F, A, B]) {
    def should(f: F[B] => F[B]): Kleisli[F, A, B] = k.mapF(f)
  }

  type PageCall[T] = Kleisli[ErrorOr, T, HtmlPage]

  def ShowPage(pageInfo: PageInfo): ErrorOr[HtmlPage] => ErrorOr[HtmlPage] = { result: ErrorOr[HtmlPage] =>
    result mustBe a[Right[_, _]]
    eventually(Timeout(Span(2, Seconds)))(result.right.value.getTitleText mustBe pageInfo.title)

    result
  }

  def OpenPage(entryPoint: EntryPoint): PageCall[WebClient] = Kleisli((webClient: WebClient) => webClient.show(entryPoint.call))

  def ClickLink(name: String): PageCall[HtmlPage] = Kleisli((page: HtmlPage) => page.clickLink(name))

  def ClickButton(id: String): PageCall[HtmlPage] = Kleisli((page: HtmlPage) => page.clickButton(id))

  def ChooseRadioButton(id: String): PageCall[HtmlPage] = Kleisli((page: HtmlPage) => page.chooseRadioButton(id))

  def SubmitForm(buttonName: String): PageCall[HtmlPage] = Kleisli((page: HtmlPage) => page.submitForm(buttonName))

}
