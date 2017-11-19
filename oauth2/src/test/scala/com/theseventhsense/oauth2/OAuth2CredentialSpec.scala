package com.theseventhsense.oauth2

import com.theseventhsense.testing.UnitSpec

class OAuth2CredentialSpec extends UnitSpec {
  val cred = OAuth2Credential(
    providerName = "test",
    accessToken = "access1",
    accessExpires = None,
    refreshToken = Some("refresh1")
  )
  "the OAuth2Credential" should {
    "update the refresh token if one is provided on update" in {
      val response = OAuth2TokenResponse(
        access_token = "access2",
        refresh_token = Some("refresh2")
      )
      cred.update(response).refreshToken mustEqual Some("refresh2")
    }
    "keep an existing refresh token if none is provided on update" in {
      val response = OAuth2TokenResponse(access_token = "access2")
      cred.update(response).refreshToken mustEqual Some("refresh1")
    }
  }

}
