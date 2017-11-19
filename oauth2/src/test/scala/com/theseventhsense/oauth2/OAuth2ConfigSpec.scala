package com.theseventhsense.oauth2

import com.theseventhsense.testing.UnitSpec
import com.typesafe.config.{Config, ConfigFactory}

/**
  * Created by erik on 3/8/17.
  */
class OAuth2ConfigSpec extends UnitSpec {
  "the OAuth2Configuration" should {
    lazy val rawConfig = ConfigFactory.defaultReference().resolve()
    lazy val config = OAuth2Config.oauth2Config(rawConfig)
    "load the raw config from reference.conf" in {
      rawConfig mustBe a[Config]
    }
    "hydrate from the reference.conf" in {
      config mustBe a[OAuth2Config]
    }
  }

}
