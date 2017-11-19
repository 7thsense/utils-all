package com.theseventhsense.oauth2

sealed trait GrantType {
  def name: String

  override def toString: String = name
}

object GrantType {

  case object Password extends GrantType {
    val name = "password"
  }

  case object Code extends GrantType {
    val name = "authorization_code"
  }

  case object ClientCredentials extends GrantType {
    val name = "client_credentials"
  }

  case object RefreshToken extends GrantType {
    val name = "refresh_token"
  }

  object GrantType {
    val all = Seq(Password, Code, ClientCredentials, RefreshToken)
  }

}
