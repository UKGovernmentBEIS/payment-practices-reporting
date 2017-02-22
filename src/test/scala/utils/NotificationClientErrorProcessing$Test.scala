package utils

import org.scalatest.{Matchers, OptionValues, WordSpecLike}

class NotificationClientErrorProcessing$Test extends WordSpecLike with Matchers with OptionValues {

  "parseNotificationMessage" should {
    "correctly parse message" in {
      val message =
        """Status code: 400 {
          |  "message": {
          |    "to": [
          |      "Not a valid email address"
          |    ]
          |  },
          |  "result": "error"
          |}""".stripMargin

      val expectedBody = NotificationClientErrorBody(NotificationClientErrorMessage(Some(List("Not a valid email address")), None), "error")

      val result = NotificationClientErrorProcessing.parseNotificationMessage(message)
      result.value.statusCode shouldBe 400
      result.value.body shouldBe expectedBody

    }
  }

}
