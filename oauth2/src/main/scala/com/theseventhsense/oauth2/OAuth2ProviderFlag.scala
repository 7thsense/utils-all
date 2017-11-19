package com.theseventhsense.oauth2

import com.theseventhsense.oauth2

/**
  * OAuth2ProviderFlags represent the possible idiosyncrasies that can be worked
  * around for various provider implementations. Wherever possible, the default
  * mode of operation is to be fully compliant with RFC 6749 and to modify
  * that behavior only in the case of a provider flag being set.
  */
sealed trait OAuth2ProviderFlag {
  def name: String
}

case object RequiresStateCookie extends OAuth2ProviderFlag {
  val name = "REQUIRES_SCOPE_COOKIE"
}

case object RequiresClientCredentialsGrantType extends OAuth2ProviderFlag {
  val name = "REQUIRES_CLIENT_CREDENTIALS_GRANT_TYPE"
}

case object RequiresClientCredentialsInTokenPayload extends OAuth2ProviderFlag {
  val name = "REQUEST_CLIENT_CREDENTIALS_IN_TOKEN_PAYLOAD"
}

case object RequiresClientIdInTokenPayload extends OAuth2ProviderFlag {
  val name = "REQUEST_CLIENT_ID_IN_TOKEN_PAYLOAD"
}

case object RequiresQueryParameterTokenPayload extends OAuth2ProviderFlag {
  val name = "REQUIRES_QUERY_PARAMETER_TOKEN_PAYLOAD"
}

/**
  * Indicates this provider does *not* provide a code per the oauth2 spec, but
  * instead directly provides the access and/or refresh tokens to the callback
  * url. For instance, Hubspot.
  *
  * http://developers.hubspot.com/docs/methods/auth/initiate-oauth
  */
case object SkipsTokenRequest extends OAuth2ProviderFlag {
  val name = "SKIPS_TOKEN_REQUEST"
}

sealed trait AuthorizationMechanismFlag extends OAuth2ProviderFlag

object AuthorizationMechanismFlag {

  /**
    * Indicates the access token must be provided as a query string parameter
    * instead of a header.
    */
  case object TokenAsQueryParameter extends AuthorizationMechanismFlag {
    val name = "ACCESS_TOKEN_AS_QUERY_PARAMETER"
  }

  /**
    * Indicates that the provider expects an authorization header of type
    * "token" instead of the usual "Bearer".
    */
  case object HeaderAsToken extends AuthorizationMechanismFlag {
    val name = "AUTHORIZATION_HEADER_AS_TOKEN"
  }

  /**
    * Indicates that the provider expects an authorization header of type
    * "Bearer".
    */
  case object HeaderAsBearer extends AuthorizationMechanismFlag {
    val name = "AUTHORIZATION_HEAD_AS_BEARER"
  }

  val default = HeaderAsBearer
  val all = Set(TokenAsQueryParameter, HeaderAsToken, HeaderAsBearer)
}

sealed trait RefreshMode

/**
  * Indicate that the provider does not support access token refresh at all.
  */
case object RefreshModeNone extends OAuth2ProviderFlag with RefreshMode {
  val name = "REFRESH_MODE_NONE"
}

/**
  * In order to get a refresh token, we will need to provide a scope parameter
  */
case object RefreshModeScope extends OAuth2ProviderFlag with RefreshMode {
  val name = "REFRESH_MODE_SCOPE"
}

/**
  * Token refresh is accomplished via a refresh token
  */
//case object RefreshModeAccessType extends OAuth2ProviderFlag with RefreshMode {
//  val name = "REFRESH_MODE_ACCESS_TYPE"
//}

/**
  * Token refresh is accomplished via client credentials
  */
case object RefreshModeClientCredentials
    extends OAuth2ProviderFlag
    with RefreshMode {
  val name = "REFRESH_MODE_CLIENT_CREDENTIALS"
}

object RefreshMode {
  val all =
    Set(RefreshModeNone, RefreshModeScope, RefreshModeClientCredentials)
}

object OAuth2ProviderFlag {
  val all = Set(
    RequiresStateCookie,
    RequiresClientCredentialsInTokenPayload,
    RequiresClientIdInTokenPayload,
    RequiresQueryParameterTokenPayload
  ) ++
    AuthorizationMechanismFlag.all ++
    RefreshMode.all

}
