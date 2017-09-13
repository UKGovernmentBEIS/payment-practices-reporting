package webspec

import cats.FlatMap
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

  case class SpecError(message: String, t: Option[Throwable] = None)
  type ErrorOr[T] = Either[SpecError, T]

  def webSpec(spec: PageCall[WebClient])(implicit wc: WebClient): Unit =
    spec run wc match {
      case Right(_) => Unit
      case Left(e)  => e.t match {
        case None    => fail(e.message)
        case Some(t) => fail(e.message, t)
      }
    }

  implicit class TrySyntax[T](t: Try[T]) {
    def toErrorOr(message: String): ErrorOr[T] = t match {
      case Success(v)         => Right(v)
      case Failure(throwable) => Left(SpecError(message, Some(throwable)))
    }
  }

  implicit class WebClientSyntax(webClient: WebClient) {
    def show(call: Call): ErrorOr[HtmlPage] =
      Try(webClient.getPage[HtmlPage](url(call))).toErrorOr("show")
  }

  /**
    * Here are a bunch of pimped methods that attempt to bring some sanity
    * to the java-style Page api
    *
    * @param page the HtmlPage to wrap.
    */
  implicit class PageSyntax(page: HtmlPage) {
    def byId[T <: HtmlElement](id: String): ErrorOr[T] =
      Try(page.getHtmlElementById[T](id)).toErrorOr("byId")

    def clickLink(name: String): ErrorOr[HtmlPage] =
      Try(page.getAnchorByName(name).click[HtmlPage]()).toErrorOr("clickLink")

    def clickButton(id: String): ErrorOr[HtmlPage] =
      Try(page.getHtmlElementById[HtmlButton](id).click[HtmlPage]()).toErrorOr("clickButton")

    def chooseRadioButton(id: String): ErrorOr[HtmlPage] = {
      for {
        radio <- Try(page.getHtmlElementById[HtmlRadioButtonInput](id))
        _ = radio.setChecked(true)
      } yield page
    }.toErrorOr(s"Could not find radio button with id '$id'")

    def submitForm(buttonName: String): ErrorOr[HtmlPage] = {
      for {
        submit <- Try(page.getElementByName[HtmlSubmitInput](buttonName))
        page = submit.click[HtmlPage]()
      } yield page
    }.toErrorOr("submitForm")

    def paragraphText(id: String): ErrorOr[String] = {
      byId[HtmlParagraph](id).map(_.getTextContent)
    }
  }

  implicit class ExtraKleisliSyntax[F[_], A, B](k: Kleisli[F, A, B]) {
    /**
      * Alias for Kleisli.andThen
      */
    def should[C](k2: Kleisli[F, B, C])(implicit F: FlatMap[F]): Kleisli[F, A, C] = k andThen k2
  }

  type PageCall[T] = Kleisli[ErrorOr, T, HtmlPage]

  def ShowPage(pageInfo: PageInfo): PageCall[HtmlPage] = Kleisli[ErrorOr, HtmlPage, HtmlPage] { page: HtmlPage =>
    Try {
      eventually(Timeout(Span(2, Seconds)))(page.getTitleText mustBe pageInfo.title)
    }.toErrorOr("").map(_ => page)
  }

  def OpenPage(entryPoint: EntryPoint): PageCall[WebClient] = Kleisli((webClient: WebClient) => webClient.show(entryPoint.call))

  def ClickLink(name: String): PageCall[HtmlPage] = Kleisli((page: HtmlPage) => page.clickLink(name))

  def ClickButton(id: String): PageCall[HtmlPage] = Kleisli((page: HtmlPage) => page.clickButton(id))

  def ChooseRadioButton(id: String): PageCall[HtmlPage] = Kleisli((page: HtmlPage) => page.chooseRadioButton(id))

  def SubmitForm(buttonName: String): PageCall[HtmlPage] = Kleisli((page: HtmlPage) => page.submitForm(buttonName))

}
