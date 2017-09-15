package webspec

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

case class SpecError(message: String, t: Option[Throwable] = None, page: Option[HtmlPage] = None)


trait WebSpec extends EitherValues {
  self: PlaySpec with Eventually with OneBrowserPerTest =>

  implicit val webClient: WebClient = new com.gargoylesoftware.htmlunit.WebClient()
  webClient.getOptions.setJavaScriptEnabled(false)

  lazy val baseUrl =
    s"http://localhost:${Helpers.testServerPort}"

  def url(s: String): String = baseUrl + s

  def url(call: Call): String = baseUrl + call.url


  def webSpec[T](spec: Scenario[T])(implicit wc: WebClient): Unit =
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
    def byId[T <: HtmlElement](id: String): ErrorOr[Option[T]] =
      Right(Try(page.getHtmlElementById[T](id)).toOption)

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
    def findTable(id: String): ErrorOr[Option[HtmlTable]] = page.byId[HtmlTable](id)

    def findForm(id: String): ErrorOr[Option[HtmlForm]] = page.byId[HtmlForm](id)

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
      byId[HtmlParagraph](id).flatMap {
        case Some(p) => Right(p.getTextContent)
        case None    => Left(SpecError(s"No paragraph found with id '$id'"))
      }
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


  def ShowPage(pageInfo: PageInfo): PageStep = Step { page: HtmlPage =>
    Try {
      eventually(Timeout(Span(2, Seconds)))(page.getTitleText mustBe pageInfo.title)
    }.toErrorOr(s"page title was '${page.getTitleText}' but expected ${pageInfo.title}").map(_ => page)
  }

  def OpenPage(entryPoint: EntryPoint): Scenario[HtmlPage] = Step((webClient: WebClient) => webClient.show(entryPoint.call))

  def ClickLink(name: String): Step[HtmlPage, HtmlPage] = Step((page: HtmlPage) => page.clickLink(name))
  def ClickButton(id: String): PageStep = Step((page: HtmlPage) => page.clickButton(id))
  def ChooseRadioButton(id: String): PageStep = Step((page: HtmlPage) => page.chooseRadioButton(id))
  def SubmitForm(buttonName: String): PageStep = Step((page: HtmlPage) => page.submitForm(buttonName))
  def SetNumberField(id: String, value: Int): PageStep = Step((page: HtmlPage) => page.setNumberField(id, value))
  def SetTextField(id: String, value: String): PageStep = Step((page: HtmlPage) => page.setTextField(id, value))

  def SetDateFields(id: String, dateFields: DateFields): PageStep =
    SetNumberField(s"$id.day", dateFields.day) andThen
      SetNumberField(s"$id.month", dateFields.month) andThen
      SetNumberField(s"$id.year", dateFields.year)

  //noinspection TypeAnnotation
  def Table(id: String) = OptionalSideStep[HtmlPage, HtmlTable] { page: HtmlPage =>
    page.findTable(id).map((page, _))
  }

  //noinspection TypeAnnotation
  def Form(id: String) = OptionalSideStep[HtmlPage, HtmlForm] { page: HtmlPage =>
    page.findForm(id).map((page, _))
  }

  //noinspection TypeAnnotation
  def Element[E <: HtmlElement](id: String) = OptionalSideStep[HtmlPage, E] { page: HtmlPage =>
    page.byId(id).map { e: Option[E] => (page, e) }
  }

  //noinspection TypeAnnotation
  def ContainText[E <: HtmlElement](text: String) = SideStep[E, String] { e: E =>
    val content = e.getTextContent
    if (content.contains(text)) Right((e, content))
    else Left(SpecError(s"Element did not contain text '$text'"))
  }
}