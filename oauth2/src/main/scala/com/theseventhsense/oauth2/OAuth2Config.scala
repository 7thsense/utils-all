package com.theseventhsense.oauth2

import com.typesafe.config.Config
import pureconfig._
import pureconfig.module.enum._

/**
  * Created by erik on 3/8/17.
  */
sealed trait ProviderConfig extends Product with Serializable {
  def provider: OAuth2Provider
}
object ProviderConfig {
  case class ActOn(clientId: String, clientSecret: String)
      extends ProviderConfig {
    override def provider: OAuth2Provider =
      OAuth2Provider.ActOn.provider(clientId, clientSecret)
  }
  case class Eloqua(clientId: String, clientSecret: String)
      extends ProviderConfig {
    override def provider: OAuth2Provider =
      OAuth2Provider.Eloqua.provider(clientId, clientSecret)
  }
  case class Github(clientId: String, clientSecret: String)
      extends ProviderConfig {
    override def provider: OAuth2Provider =
      OAuth2Provider.Github.provider(clientId, clientSecret)
  }
  case class Google(clientId: String, clientSecret: String)
      extends ProviderConfig {
    override def provider: OAuth2Provider =
      OAuth2Provider.Google.provider(clientId, clientSecret)
  }
  case class Hubspot(clientId: String, clientSecret: String)
      extends ProviderConfig {
    override def provider: OAuth2Provider =
      OAuth2Provider.HubspotOAuth1.provider(clientId, clientSecret)
  }
  case class HubspotOAuth2(clientId: String, clientSecret: String)
      extends ProviderConfig {
    override def provider: OAuth2Provider =
      OAuth2Provider.HubspotOAuth2.provider(clientId, clientSecret)
  }
  case class LinkedIn(clientId: String, clientSecret: String)
      extends ProviderConfig {
    override def provider: OAuth2Provider =
      OAuth2Provider.LinkedIn.provider(clientId, clientSecret)
  }
  case class Marketo() extends ProviderConfig {
    override def provider: OAuth2Provider = OAuth2Provider.Marketo.baseProvider
  }
  case class Salesforce(clientId: String, clientSecret: String)
      extends ProviderConfig {
    override def provider: OAuth2Provider =
      OAuth2Provider.Salesforce.provider(clientId, clientSecret)
  }
}
case class OAuth2Config(providers: Set[ProviderConfig])

object OAuth2Config {
  def config(config: Config): Config =
    config.getConfig("com.theseventhsense.oauth2")

  def oauth2Config(config: Config): OAuth2Config =
    loadConfigOrThrow[OAuth2Config](this.config(config))

}
