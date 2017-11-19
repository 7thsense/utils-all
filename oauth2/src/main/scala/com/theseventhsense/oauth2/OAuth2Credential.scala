package com.theseventhsense.oauth2

import com.theseventhsense.utils.persistence.DomainObject
import com.theseventhsense.utils.types.SSDateTime

case class OAuth2Credential(providerName: String,
                            accessToken: String,
                            accessExpires: Option[SSDateTime.Instant] = None,
                            refreshToken: Option[String] = None,
                            authUrl: Option[String] = None,
                            tokenUrl: Option[String] = None,
                            clientId: Option[String] = None,
                            clientSecret: Option[String] = None,
                            cTime: SSDateTime.Instant = SSDateTime.Instant.now,
                            mTime: SSDateTime.Instant = SSDateTime.Instant.now,
                            id: OAuth2Id = OAuth2Id.Default)
    extends DomainObject {
  def requireRefreshToken(): Unit = {
    require(
      refreshToken.isDefined,
      s"Refresh token is required and not configured for $id, provider $providerName}"
    )
  }

  def update(oAuth2TokenResponse: OAuth2TokenResponse): OAuth2Credential = {
    copy(
      accessToken = oAuth2TokenResponse.access_token,
      accessExpires = oAuth2TokenResponse.accessExpires,
      refreshToken = oAuth2TokenResponse.refresh_token.orElse(this.refreshToken),
      mTime = SSDateTime.Instant.now
    )
  }

  def credentialsOverride: Option[OAuth2CredentialsOverride] =
    for {
      cId <- clientId
      cSecret <- clientSecret
    } yield
      OAuth2CredentialsOverride(
        clientId = cId,
        clientSecret = cSecret,
        authUrl = authUrl,
        tokenUrl = tokenUrl
      )
}

object OAuth2Credential {
  def from(provider: OAuth2Provider,
           oAuth2TokenResponse: OAuth2TokenResponse): OAuth2Credential = {
    OAuth2Credential(
      providerName = provider.name,
      accessToken = oAuth2TokenResponse.access_token,
      accessExpires = oAuth2TokenResponse.accessExpires,
      refreshToken = oAuth2TokenResponse.refresh_token
    )
  }

}
