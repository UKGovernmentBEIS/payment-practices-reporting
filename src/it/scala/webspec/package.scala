import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage

package object webspec {
  type ErrorOr[T] = Either[SpecError, T]
  type Scenario[T] = Step[WebClient, T]
  type PageStep = Step[HtmlPage, HtmlPage]
}
