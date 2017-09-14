package webspec

import cats.FlatMap
import cats.data.Kleisli
import cats.instances.either._
import cats.syntax.either._
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html._
import forms.DateFields
import org.scalatest.EitherValues
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.{OneBrowserPerTest, PlaySpec}
import play.api.mvc.Call
import play.api.test.Helpers

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

trait WebSpec extends EitherValues {
  self: PlaySpec with Eventually with OneBrowserPerTest =>

  implicit val webClient: WebClient = new com.gargoylesoftware.htmlunit.WebClient()
  webClient.getOptions.setJavaScriptEnabled(false)

  lazy val baseUrl =
    s"http://localhost:${Helpers.testServerPort}"

  def url(s: String): String = baseUrl + s

  def url(call: Call): String = baseUrl + call.url

  case class SpecError(message: String, t: Option[Throwable] = None, page: Option[HtmlPage])
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
    def toErrorOr(message: String, page: Option[HtmlPage] = None): ErrorOr[T] = t match {
      case Success(v)         => Right(v)
      case Failure(throwable) => Left(SpecError(message, Some(throwable), page))
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

    /**
      * @return first table found in the page
      */
    def findTable: ErrorOr[HtmlTable] =
      page.getElementsByTagName("table").toList.headOption match {
        case None        => Left(SpecError("No tables found in page", None, Some(page)))
        case Some(table) => Right(table.asInstanceOf[HtmlTable])
      }

    /**
      * @return first table found in the page
      */
    def findTable(id: String): ErrorOr[HtmlTable] = page.byId[HtmlTable](id)

    def clickLink(name: String): ErrorOr[HtmlPage] =
      Try(page.getAnchorByName(name).click[HtmlPage]()).toErrorOr("clickLink")

    def clickButton(id: String): ErrorOr[HtmlPage] =
      Try(page.getHtmlElementById[HtmlButton](id).click[HtmlPage]()).toErrorOr("clickButton")

    def chooseRadioButton(id: String): ErrorOr[HtmlPage] = {
      for {
        radio <- Try(page.getHtmlElementById[HtmlRadioButtonInput](id))
        _ = radio.setChecked(true)
      } yield page
    }.toErrorOr(s"Could not find radio button with id '$id'. Page title is ${page.getTitleText}")

    def submitForm(buttonName: String): ErrorOr[HtmlPage] = {
      for {
        submit <- Try(page.getElementByName[HtmlSubmitInput](buttonName))
        page = submit.click[HtmlPage]()
      } yield page
    }.toErrorOr("submitForm")

    def paragraphText(id: String): ErrorOr[String] = {
      byId[HtmlParagraph](id).map(_.getTextContent)
    }

    def containsElementWithId[E <: HtmlElement](id: String): ErrorOr[HtmlPage] = Try {
      page.getHtmlElementById[E](id)
    }.toErrorOr(s"Element with id '$id' was not found", Some(page)).map(_ => page)

    def findElementWithId[E <: HtmlElement](id: String): ErrorOr[E] = Try {
      page.getHtmlElementById[E](id)
    }.toErrorOr(s"Element with id '$id' was not found", Some(page))

    def setTextField(id: String, value: String): ErrorOr[HtmlPage] = for {
      e <- findElementWithId[HtmlTextInput](id)
      _ <- Try(e.setText(value)).toErrorOr(s"could not set text for element with id $id")
    } yield page

    def setNumberField(id: String, value: Int): ErrorOr[HtmlPage] = for {
      e <- findElementWithId[HtmlNumberInput](id)
      _ <- Try(e.setText(value.toString)).toErrorOr(s"could not set text for element with id $id")
    } yield page
  }

  implicit class TableSyntax(table: HtmlTable) {
    def getRowWithName(rowName: String): ErrorOr[HtmlTableRow] =
      table.getRows.toList.find { row =>
        row.getCell(0).getTextContent.trim === rowName
      } match {
        case None      => Left(SpecError(s"table does not have a row with name '$rowName'", None, None))
        case Some(row) => Right(row)
      }
  }

  implicit class ElementSyntax[E1, E2 <: HtmlElement](k: Kleisli[ErrorOr, E1, (E1, E2)]) {
    def should(k2: Kleisli[ErrorOr, E2, E2]): Kleisli[ErrorOr, E1, E1] = {
      k.flatMapF { case (t1, t) =>
        val value1: ErrorOr[E2] = k2.run(t)
        value1.map(_ => t1)
      }
    }

    def having(k2: Kleisli[ErrorOr, E2, E2]): Kleisli[ErrorOr, E1, E1] = should(k2)
  }

  implicit class ExtraKleisliSyntax[F[_], A, B](k: Kleisli[F, A, B]) {
    /**
      * Aliases for Kleisli.andThen
      */
    def should[C](k2: Kleisli[F, B, C])(implicit F: FlatMap[F]): Kleisli[F, A, C] = k andThen k2

    def and[C](k2: Kleisli[F, B, C])(implicit F: FlatMap[F]): Kleisli[F, A, C] = k andThen k2

    def where[C](k2: Kleisli[F, B, C])(implicit F: FlatMap[F]): Kleisli[F, A, C] = k andThen k2
  }

  implicit class PageCallSyntax[A](k: PageCall[A]) {
    def withElementById[E <: HtmlElement](id: String): PageCall[A] =
      k andThen Kleisli[ErrorOr, HtmlPage, HtmlPage]((page: HtmlPage) => page.containsElementWithId[E](id))

    /**
      * Try to find an element by id and then check that it matches a predicate
      *
      * @param id   the id of the element to find
      * @param pred a predicate to check a condition on the element
      * @tparam E the type of the element to find (a subclass of HtmlElement)
      * @return a new PageCall composed of the wrapped call andThen the element test
      */
    def containingElement[E <: HtmlElement](id: String)(pred: E => Boolean): PageCall[A] =
      k andThen Kleisli[ErrorOr, HtmlPage, HtmlPage] { page: HtmlPage =>
        for {
          e <- page.findElementWithId[E](id)
          _ <- if (pred(e)) Right(e) else Left(SpecError("element did not satisfy predicate", None, None))
        } yield page
      }
  }

  type PageCall[T] = Kleisli[ErrorOr, T, HtmlPage]

  def ShowPage(pageInfo: PageInfo): PageCall[HtmlPage] = Kleisli[ErrorOr, HtmlPage, HtmlPage] { page: HtmlPage =>
    Try {
      eventually(Timeout(Span(2, Seconds)))(page.getTitleText mustBe pageInfo.title)
    }.toErrorOr(s"page title was '${page.getTitleText}' but expected ${pageInfo.title}").map(_ => page)
  }

  def OpenPage(entryPoint: EntryPoint): PageCall[WebClient] = Kleisli((webClient: WebClient) => webClient.show(entryPoint.call))
  def ClickLink(name: String): PageCall[HtmlPage] = Kleisli((page: HtmlPage) => page.clickLink(name))
  def ClickButton(id: String): PageCall[HtmlPage] = Kleisli((page: HtmlPage) => page.clickButton(id))
  def ChooseRadioButton(id: String): PageCall[HtmlPage] = Kleisli((page: HtmlPage) => page.chooseRadioButton(id))
  def SubmitForm(buttonName: String): PageCall[HtmlPage] = Kleisli((page: HtmlPage) => page.submitForm(buttonName))
  def SetNumberField(id: String, value: Int): PageCall[HtmlPage] = Kleisli((page: HtmlPage) => page.setNumberField(id, value))
  def SetTextField(id: String, value: String): PageCall[HtmlPage] = Kleisli((page: HtmlPage) => page.setTextField(id, value))

  def SetDateFields(id: String, dateFields: DateFields): PageCall[HtmlPage] =
    SetNumberField(s"$id.day", dateFields.day) andThen
      SetNumberField(s"$id.month", dateFields.month) andThen
      SetNumberField(s"$id.year", dateFields.year)

  //noinspection TypeAnnotation
  def Table(id: String) = Kleisli[ErrorOr, HtmlPage, (HtmlPage, HtmlTable)] { page =>
    page.findTable(id).map((page, _))
  }


}