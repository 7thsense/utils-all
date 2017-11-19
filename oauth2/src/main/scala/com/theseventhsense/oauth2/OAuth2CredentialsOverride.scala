package com.theseventhsense.oauth2

import play.api.mvc.RequestHeader

/**
  * Created by erik on 4/18/16.
  */
case class OAuth2CredentialsOverride(clientId: String,
                                     clientSecret: String,
                                     authUrl: Option[String],
                                     tokenUrl: Option[String])

object OAuth2CredentialsOverride {
  def fromRequest(request: RequestHeader): Option[OAuth2CredentialsOverride] = {
    for {
      clientId <- request.getQueryString("clientId")
      clientSecret <- request.getQueryString("clientSecret")
    } yield
      OAuth2CredentialsOverride(
        clientId,
        clientSecret,
        request.getQueryString("authUrl"),
        request.getQueryString("tokenUrl")
      )
  }
}
