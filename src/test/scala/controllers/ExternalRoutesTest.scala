package controllers

import config.RoutesConfig
import org.scalatest.{Matchers, WordSpecLike}

class ExternalRoutesTest extends WordSpecLike with Matchers {

  "ExternalRoutes" should {
    "generate a router for localhost when no search host is provided" in {
      val r = (new ExternalRoutes(new RoutesConfig(None))) ("localhost:9000")
      r.root shouldBe "http://localhost:9001"
    }

    "generate a router for the supplied search host" in {
      val r = (new ExternalRoutes(new RoutesConfig(Some("otherhost")))) ("thishost")
      r.root shouldBe "https://otherhost"
    }

    "generate a router for the corresponding host when the supplied host matches the heroku pattern" in {
      val r = (new ExternalRoutes(new RoutesConfig(None))) ("beis-ppr-dev.herokuapp.com")
      r.root shouldBe "https://beis-spp-dev.herokuapp.com"
    }

    "generate a router for the configured host even if the supplied host matches the heroku pattern" in {
      val r = (new ExternalRoutes(new RoutesConfig(Some("otherhost")))) ("beis-ppr-dev.herokuapp.com")
      r.root shouldBe "https://otherhost"
    }

  }

}
