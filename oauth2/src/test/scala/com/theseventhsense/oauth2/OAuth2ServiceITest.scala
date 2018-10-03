package com.theseventhsense.oauth2

import com.google.inject.Guice

import com.theseventhsense.testing.tags.ExternalAPI
import com.theseventhsense.testing.{AkkaUnitSpec, WSClientFactory}
import net.codingwell.scalaguice.InjectorExtensions._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

import com.theseventhsense.utils.logging.LogContext

trait OAuth2ServiceITest
    extends AkkaUnitSpec
    with WSClientFactory
    with ScalaFutures
    with IntegrationPatience {
  def hubspotToken: String

  def googleToken: String

  def salesforceToken: String

  def actOnUsername: String

  def actOnPassword: String

  def actOnCreds: OAuth2Credential

  implicit val lc: LogContext = LogContext.empty

  val injector = Guice.createInjector(new OAuth2Module)
  implicit val service: OAuth2Service = injector.instance[OAuth2Service]
  implicit val store: TOAuth2Persistence = injector.instance[TOAuth2Persistence]
  lazy val providers = injector.instance[Set[OAuth2Provider]]

  private def getCreds(providerName: String, refreshToken: String) = {
    OAuth2Credential(
      id = OAuth2Id(1),
      providerName = providerName,
      accessToken = "invalid",
      refreshToken = Some(refreshToken)
    )
  }

  private def testTokenRefresh(creds: OAuth2Credential) = {
    val req = service.refresh(OAuth2Id(1))
    val response = req.futureValue
    response.accessToken must not equal creds.accessToken
  }

  "the OAuth2Service" ignore {
    "be able to refresh hubspot tokens" taggedAs ExternalAPI in {
      testTokenRefresh(getCreds("hubspot", hubspotToken))
    }
    "be able to refresh google tokens" taggedAs ExternalAPI in {
      testTokenRefresh(getCreds("google", googleToken))
    }
    "be able to refresh salesforce tokens" taggedAs ExternalAPI in {
      testTokenRefresh(getCreds("salesforce", salesforceToken))
    }
  }
  "the Act-On provider" ignore {
    val provider = providers.find(_.name == "acton").get
    lazy val passwordCreds = service
      .passwordGrantAccessToken(provider, actOnUsername, actOnPassword)
      .futureValue
    lazy val firstRefreshedCreds = service.refresh(OAuth2Id(1)).futureValue
    lazy val secondRefreshedCreds = service.refresh(OAuth2Id(1)).futureValue

    "be able to issue a password grant" in {
      logger.debug(s"Credential from password grant: $passwordCreds")
      passwordCreds.accessToken must not equal ""
      passwordCreds.refreshToken.value must not equal ""
    }

    "be able to refresh the newly issue grant" in {
      logger.debug(s"Credential from refresh grant: $firstRefreshedCreds")
      firstRefreshedCreds must not equal passwordCreds
      firstRefreshedCreds.refreshToken must not equal passwordCreds.refreshToken
    }

    "be able to use the credentials returned from the refresh grant" in {
      logger.debug(s"Credential from 2nd refresh grant: $secondRefreshedCreds")
      secondRefreshedCreds must not equal firstRefreshedCreds
      secondRefreshedCreds.refreshToken must not equal secondRefreshedCreds
    }

    "not be able to re-use the previously issued refresh token" in {
      service
        .refresh(OAuth2Id(1))
        .failed
        .futureValue mustBe an[OAuth2Exception]
    }
  }
}
