package questionnaire

import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.{RequestHeader, Result}

object DummyMessages extends MessagesApi {
  override def messages: Map[String, Map[String, String]] = ???

  override def preferred(candidates: Seq[Lang]): Messages = ???

  override def preferred(request: RequestHeader): Messages = ???

  override def preferred(request: play.mvc.Http.RequestHeader): Messages = ???

  override def setLang(result: Result, lang: Lang): Result = ???

  override def clearLang(result: Result): Result = ???

  override def apply(key: String, args: Any*)(implicit lang: Lang): String = ""

  override def apply(keys: Seq[String], args: Any*)(implicit lang: Lang): String = ???

  override def translate(key: String, args: Seq[Any])(implicit lang: Lang): Option[String] = ???

  override def isDefinedAt(key: String)(implicit lang: Lang): Boolean = ???

  override def langCookieName: String = ???

  override def langCookieSecure: Boolean = ???

  override def langCookieHttpOnly: Boolean = ???
}
