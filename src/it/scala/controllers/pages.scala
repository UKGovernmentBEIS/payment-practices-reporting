package controllers

import java.net.URLEncoder
import javax.inject.Inject

import org.jsoup.nodes.{Document, Element}
import org.scalactic.TripleEquals._
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Call
import questionnaire.Questions.isCompanyOrLLPQuestion

import scala.collection.JavaConversions._
import scala.concurrent.Future

trait PageInfo {

  /**
    * @return an name that identifies the page in testing. It does not need to
    *         correspond to any name or text for the page itself.
    */
  def name: String

  def call: Call

  /**
    * @return the title that appears on the page itself
    */
  def title: String

  def open(implicit client: WebClient): Future[Document] = client.get[Document](call)
}

case class Form(element: Element) {
  implicit class ElementSyntax(element: Element) {
    def attrO(attrName: String): Option[String] =
      if (element.hasAttr(attrName)) Some(element.attr(attrName))
      else None
  }

  def values: Map[String, Seq[String]] = {
    val vs: Seq[(String, String)] = element.getElementsByTag("input").toList.flatMap { e =>
      e.attr("type").toLowerCase match {
        case "radio" if e.hasAttr("selected") => Some(e.attr("name") -> e.attr("value"))
        case "radio"                          => None
        case _                                => Some(e.attr("name") -> e.attr("value"))
      }
    }

    vs.groupBy(_._1).map { case (key, ks) => (key, ks.map(_._2)) }
  }

  def selectRadio(id: String): Form = {
    Option(element.getElementById(id)) match {
      case None        => this
      case Some(radio) =>
        val cloned = element.clone()
        val choices = cloned.getElementsByAttributeValue("name", radio.attr("name"))
        choices.toList.map { r =>
          if (r.id === id) r.attr("selected", "")
          else r.removeAttr("selected")
        }
        Form(cloned)
    }
  }

  def method: String = element.attrO("method").map(_.toUpperCase).getOrElse("GET")

  def action: String = element.attrO("action").getOrElse("")

  def urlEncoded: String = values.flatMap(item => item._2.map(c => item._1 + "=" + URLEncoder.encode(c, "UTF-8"))).mkString("&")

  def submit(implicit webClient: WebClient): Future[Document] = method match {
    case "POST" => webClient.postForm[Document](action)(urlEncoded)
  }

}

trait Page {
  def pageInfo: PageInfo
  def document: Document
  def title: String = document.title

  def form(id: String): Option[Form] = {
    Option(document.getElementById(id)).flatMap { e =>
      e.tagName match {
        case "form" => Some(Form(e))
        case _      => None
      }
    }
  }

  def errors: Seq[String] = document.getElementsByClass("error-summary-list").toList.map(li => li.child(0).`val`)
}

object QuestionnaireStartPageInfo extends PageInfo {
  val name : String = "Questionnaire Start Page"
  val call : Call   = routes.QuestionnaireController.start()
  val title: String = QuestionnaireController.startTitle
}

case class CompanyOrLLPQuestionPageInfo(messages: MessagesApi) extends PageInfo {
  override val name = "Company or LLP Question"

  override def call: Call = ???

  override val title = messages(isCompanyOrLLPQuestion.textKey)
}

object NoNeedToReportPageInfo extends PageInfo {
  override val name: String = "No need to report"

  override def call: Call = ???

  override val title: String = QuestionnaireController.exemptTitle
}

case class NoNeedToReportPage(override val document: Document) extends Page {
  override val pageInfo: PageInfo = NoNeedToReportPageInfo
}

case class QuestionnaireStartPage(override val document: Document) extends Page {
  override val pageInfo: PageInfo = QuestionnaireStartPageInfo
}

case class CompanyOrLLPQuestionPage(override val document: Document, messages: MessagesApi) extends Page {
  override val pageInfo: PageInfo = CompanyOrLLPQuestionPageInfo(messages)
}

class Pages @Inject()(messages: MessagesApi)(implicit webClient: WebClient) {
  def questionnaireStartPage: Future[QuestionnaireStartPage] = QuestionnaireStartPageInfo.open.map(QuestionnaireStartPage)
}