package forms

import forms.Validations.words
import org.scalatest.{EitherValues, Matchers, WordSpecLike}
import play.api.data.Forms._

class Validations$Test extends WordSpecLike with Matchers with EitherValues {

  "words validation" should {
    val m = single("test" -> words(maxWords = 5))

    "be successful when word limit not exceeded" in {
      val s = "only three words"
      val result = m.bind(Map("test" -> s))

      result.right.value shouldBe s
    }

    "fail when word limit is exceeded" in {
      val s = "here we have six little words"
      val result = m.bind(Map("test" -> s))

      result shouldBe a[Left[_, _]]
    }

    "fail when the words count is okay but overall string is too long" in {
      // average word length is taken to be 7, so five words should not exceed 35 characters, including whitespace
      val s = "herefore wordcount numbering five butverylongones"
      val result = m.bind(Map("test" -> s))

      result shouldBe a[Left[_, _]]
    }
  }

}
