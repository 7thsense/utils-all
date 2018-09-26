package com.theseventhsense.oauth2

import com.theseventhsense.oauth2.OAuth2Provider.DefaultOAuth2Provider
import com.theseventhsense.testing.{AkkaUnitSpec, UnitSpec, WSClientFactory}
import com.theseventhsense.utils.types.SSDateTime
import mockws.MockWS
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global

class OAuth2ServiceSpec
    extends AkkaUnitSpec
    with ScalaFutures
    with WSClientFactory
    with IntegrationPatience {
  val testProvider = DefaultOAuth2Provider(
    name = "test",
    clientId = Option("clientId"),
    clientSecret = Option("clientSecret"),
    authUrl = Option("http://oauth2/auth"),
    tokenUrl = Option("http://oauth2/token")
  )
  def init: (OAuth2Credential, OAuth2Service) = {
    implicit val testPersistence = new InMemoryOAuth2Persistence()
    val cred1 = testPersistence
      .create(
        new OAuth2Credential(
          providerName = "test",
          accessToken = "invalid1",
          accessExpires = Some(SSDateTime.now.minusYears(1)),
          refreshToken = Some("refresh1")
        )
      )
      .futureValue
    (cred1, new OAuth2Service(Set(testProvider)))
  }
  "the OAuth2Service" should {
    "keep existing refresh tokens if none provided" in {
      val (cred1, service) = this.init
      implicit val ws = MockWS {
        case ("GET", "http://oauth2/auth") =>
          Action {
            Ok("auth")
          }
        case ("POST", "http://oauth2/token") =>
          Action {
            Ok(
              Json.obj(
                "token_type" -> "bearer",
                "expires_in" -> 3600,
                "access_token" -> "invalid2"
              )
            )
          }
      }
      val updatedCred = service.refresh(cred1.id).futureValue
      updatedCred.accessToken must not equal cred1.accessToken
      updatedCred.refreshToken mustEqual cred1.refreshToken
    }
    "update refresh tokens if provided" in {
      val (cred1, service) = this.init
      implicit val ws = MockWS {
        case ("POST", "http://oauth2/token") =>
          Action {
            Ok(
              Json.obj(
                "token_type" -> "bearer",
                "expires_in" -> 3600,
                "refresh_token" -> "refresh2",
                "access_token" -> "invalid2"
              )
            )
          }
      }
      val updatedCred = service.refresh(cred1.id).futureValue
      updatedCred.accessToken must not equal cred1.accessToken
      updatedCred.refreshToken must not equal cred1.refreshToken
    }
    "update refresh credentials when they are provided" in {
      val ws = MockWS {
        case ("GET", "http://oauth2/auth")  => Action { Ok("index") }
        case ("GET", "http://oauth2/auth2") => Action { NotFound("nothing") }
        case ("GET", "http://oauth2/token") => Action { Ok("world") }
      }
    }
  }

}
