package views.html

import play.api.data.Field
import play.api.i18n.MessagesApi

object FieldHelpers {
  def errorMessage(field: Field)(implicit messages: MessagesApi) =
    field.error.map(e => messages(e.message, e.args: _*))

  def errorClass(field: Field) = if (field.hasErrors) "error" else ""

}
