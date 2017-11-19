package com.theseventhsense.oauth2

abstract class AuthParam extends Product with Serializable {
  def name: String
  override def toString: String = name
}

object AuthParam {
  case class Custom(name: String) extends AuthParam
  case object ClientId extends AuthParam {
    override val name = "clientId"
  }
  case object ClientSecret extends AuthParam {
    override val name = "clientSecret"
  }
  case object AuthUrl extends AuthParam {
    override val name = "authUrl"
  }
  case object TokenUrl extends AuthParam {
    override val name = "tokenUrl"
  }
  val ClientCredentialsParams = Seq(ClientId, ClientSecret, TokenUrl)
}
