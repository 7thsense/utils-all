package com.github.easel.auth0

import java.net.URL

import cats.scalatest.EitherValues
import com.auth0.jwk.{Jwk, JwkProvider, UrlJwkProvider}
import com.theseventhsense.testing.UnitSpec

class JWTAuthorizerSpec extends UnitSpec with EitherValues {
  val idToken =
    "eyJhbGciOiJSUzI1NiIsImtpZCI6ImY0MzYwNWRlYzY5YjdmN2U1YThiNWY2ZDIzZjM5YTMwYWE1YWY2ZTcifQ.eyJhenAiOiIxNDE0Mjk3MzEzNjEtdWg4ZHJnOGM4ZG1jbGxzM2dtaWVpaHQzZTBob2NsY2QuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiIxNDE0Mjk3MzEzNjEtdWg4ZHJnOGM4ZG1jbGxzM2dtaWVpaHQzZTBob2NsY2QuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDY3NDg2MTQ1ODk0OTYxMTIxMjAiLCJoZCI6InRlbGVwYXRoZGF0YS5jb20iLCJlbWFpbCI6ImVyaWtAdGVsZXBhdGhkYXRhLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJhdF9oYXNoIjoiWFJwNHZVVVdnOFJPWEdqVjJ0SkFmUSIsImlzcyI6ImFjY291bnRzLmdvb2dsZS5jb20iLCJpYXQiOjE1MTA5NjE4NjIsImV4cCI6MTUxMDk2NTQ2Mn0.BJKybEKvDsYNobFluO_V0JNrcubJQKKrzAIYgg57hgVhB0DJaPeJK5WcwBm71CJy6Hw5BRIYYlBevROUlVlGe93l_BWHIHDQ3_A49Ks42htB2riZnCX4GYy1gO0j2r88I99dlVlERXhpnHd9B0EktFlR11er1GdzMDFPjSL_Lj4PWxEJL2yeVAjbCyN3n4rBg1SGJSNB77KY_S4bfzz02JQ9XecahAWdqIw_csp5x_CqLknV4MEwsDF8wcjnZhVDvtTJV7Fmc_b-KeomYKf_uCD0Yuy7PIPv4ZqCceTrcm5ClFExcPkB-kvdiZ7ZJuUl0LnBLmKpwt6HLssEZvyIdg"

  lazy val authorizer = new JWTAuthorizer()
  "the JWTAuthorizer" should {
    "create a jwkProvider from a google jwks url" in {
      val provider = new UrlJwkProvider(new URL("https://www.googleapis.com/oauth2/v3/certs"))
      provider.get("f43605dec69b7f7e5a8b5f6d23f39a30aa5af6e7") mustBe a[Jwk]
    }
    "create a jwkProvider from a discover document url" ignore {
      val provider = new UrlJwkProvider(new URL("https://accounts.google.com/.well-known/openid-configuration"))
      provider.get("f43605dec69b7f7e5a8b5f6d23f39a30aa5af6e7") mustBe 'defined
    }
    "provide a valid google JWTProvider via the discovery document" ignore {
      val provider = authorizer.jwkProvider("https://accounts.google.com/.well-known/openid-configuration")
      provider.get("f43605dec69b7f7e5a8b5f6d23f39a30aa5af6e7")
    }
    "provide a valid google JWTProvider" in {
      val provider = authorizer.jwkProvider("accounts.google.com")
      provider.get("f43605dec69b7f7e5a8b5f6d23f39a30aa5af6e7") mustBe a[Jwk]
    }
    "be able to decode google idtoken" in {
      pending
      val idToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImY0MzYwNWRlYzY5YjdmN2U1YThiNWY2ZDIzZjM5YTMwYWE1YWY2ZTcifQ.eyJhenAiOiIxNDE0Mjk3MzEzNjEtdWg4ZHJnOGM4ZG1jbGxzM2dtaWVpaHQzZTBob2NsY2QuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiIxNDE0Mjk3MzEzNjEtdWg4ZHJnOGM4ZG1jbGxzM2dtaWVpaHQzZTBob2NsY2QuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDY3NDg2MTQ1ODk0OTYxMTIxMjAiLCJoZCI6InRlbGVwYXRoZGF0YS5jb20iLCJlbWFpbCI6ImVyaWtAdGVsZXBhdGhkYXRhLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJhdF9oYXNoIjoiLWsyYXNkeTA0VlVYSUpxUnctdUVldyIsImlzcyI6ImFjY291bnRzLmdvb2dsZS5jb20iLCJpYXQiOjE1MTA5Nzg5NjIsImV4cCI6MTUxMDk4MjU2Mn0.Icag0wq0am3wJck249u7XCvl-93PSc8QViJYwoDpUsxHERkVYbEYEvxh1hIPJnaDyUEbl39mdzHceUYw9Mn8nrYtnCVjmItcx-6UTpCjKGCYMTWAcL1RKwOyh44dJ5FwCH9lU-Yl0XQxyBiUNx3r5ojmg7oXZ0gJ3JhPt8hwK6zo-0viXHWjH Lwj0LVqQEST9iw0Gq-O88UTiqSHDkNHBBICEb7NRBByKJe1xA2Wk3A8xpqIHcol58ChyYjfQQS8DT1vx38LYET2w61E5QVtQDTiJJ5LfGCYapAsO9VTPUYLT0wSXm2O9QyLMobLh1DRJDDZcTGgkQi6Fv23GNhwgQ"
      authorizer.verify(idToken).value.claims("email").asString mustEqual "erik@telepathdata.com"
    }
  }

}
