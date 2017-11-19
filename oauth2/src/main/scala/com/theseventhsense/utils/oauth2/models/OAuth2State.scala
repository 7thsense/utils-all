package com.theseventhsense.utils.oauth2.models

import com.theseventhsense.oauth2.{OAuth2CredentialsOverride, OAuth2Id}
import com.theseventhsense.utils.persistence.AkkaMessage

case class OAuth2State(id: String,
                       providerName: String,
                       oAuth2Id: Option[OAuth2Id] = None,
                       next: Option[String] = None,
                       bindUrl: Option[String] = None,
                       oAuth2Override: Option[OAuth2CredentialsOverride] = None) extends AkkaMessage
