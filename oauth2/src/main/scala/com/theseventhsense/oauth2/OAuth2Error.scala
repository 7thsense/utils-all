package com.theseventhsense.oauth2

import play.api.libs.json.Json

case class OAuth2Error(error: String, message: String)

object OAuth2Error {
  implicit lazy val format = Json.format[OAuth2Error]
}
