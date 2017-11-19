package com.theseventhsense.oauth2

import com.theseventhsense.utils.types.SSDateTime

case class OAuth2TokenResponse(access_token: String,
                               token_type: Option[String] = None,
                               id: Option[String] = None,
                               issued_at: Option[SSDateTime.Instant] = None,
                               expires_in: Option[Int] = None,
                               signature: Option[String] = None,
                               instance_url: Option[String] = None,
                               id_token: Option[String] = None,
                               scope: Option[String] = None,
                               refresh_token: Option[String] = None,
                               raw: Option[String] = None) {
  def accessExpires: Option[SSDateTime.Instant] = {
    expires_in.map(_.toLong).map(SSDateTime.now.plusSeconds)
  }
}
