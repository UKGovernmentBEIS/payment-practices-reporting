package utils

import org.scalatest.{Matchers, OptionValues, WordSpecLike}

class Ordinals$Test extends WordSpecLike with Matchers with OptionValues{

  "Ordinals" should {
    "return 'first'" in {
      Ordinals.forNumber(1).value shouldBe "first"
    }

    "return 'tenth'" in {
      Ordinals.forNumber(10).value shouldBe "tenth"
    }

    "return None" in {
      Ordinals.forNumber(0) shouldBe None
      Ordinals.forNumber(-1) shouldBe None
    }
  }

}
