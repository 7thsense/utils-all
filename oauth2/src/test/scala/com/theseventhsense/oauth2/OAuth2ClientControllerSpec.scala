package com.theseventhsense.oauth2

import cats.implicits._
import com.theseventhsense.oauth2.OAuth2Codecs._
import com.theseventhsense.oauth2.OAuth2Provider.DefaultOAuth2Provider
import com.theseventhsense.testing.UnitSpec
import com.theseventhsense.utils.oauth2.models.OAuth2State
import io.circe.syntax._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OAuth2ClientControllerSpec extends UnitSpec with ScalaFutures {
  val Host = "client.local"
  val RemoteHost = "server.local"
  val CallbackUrlPath = "/connector/callback"
  val BindUrlPath = "/connector/bind"
  val BindUrl = s"https://$Host/$BindUrlPath"
  val CookieName = "testCookie"
  val ProviderName = "testProvider"
  val State = "abc"
  val Code = "def"
  val AccessToken = "access123"

  implicit val wsClient: WSClient = mock[WSClient](RETURNS_SMART_NULLS)

  "the OAuth2ClientController" when {
    "configured as a default provider" should {
      val mapper = mock[TOAuth2StateMapper](RETURNS_SMART_NULLS)
      val service = mock[OAuth2Service](RETURNS_SMART_NULLS)
      val provider =
        DefaultOAuth2Provider(
          ProviderName,
          Option("cid1"),
          Option("csc1"),
          Option(s"https://$RemoteHost/auth/$ProviderName")
        )
      when(service.knownProvider(ProviderName)).thenReturn(Some(provider))
      val state = OAuth2State(State, ProviderName)
      when(
        mapper.create(
          Matchers.any[OAuth2Provider],
          Matchers.eq(None),
          Matchers.eq(None),
          Matchers.eq(None),
          Matchers.eq(None)
        )
      ).thenReturn(state)
      val controller =
        new OAuth2ClientController(
          CallbackUrlPath,
          CookieName,
          mapper,
          service,
          stubControllerComponents()
        )
      val redirect =
        controller.auth(ProviderName).apply(FakeRequest()).futureValue

      "not set a cookie prior to redirecting to the authorization url" in {
        redirect.header.headers.keys must not contain "Set-Cookie"
      }

      "redirect the user to their authorization url" in {
        redirect.header.status mustEqual 302
        redirect.header.headers("Location") must include(provider.authUrl.get)
        ()
      }
    }
  }
  "configured to maintain state with a cookie using a bindurl" should {
    val providerFlags = Seq(RequiresStateCookie)
    val provider = DefaultOAuth2Provider(
      ProviderName,
      Option("cid1"),
      Option("csc1"),
      authUrl = Option(s"https://$RemoteHost/auth/$ProviderName"),
      tokenUrl = Option(s"https://$RemoteHost/auth/$ProviderName/token"),
      flags = providerFlags
    )
    val tokenResponse = OAuth2TokenResponse(AccessToken)
    val state = OAuth2State(State, ProviderName, bindUrl = Some(BindUrl))
    val cred = OAuth2Credential(ProviderName, AccessToken)

    val service = mock[OAuth2Service](RETURNS_SMART_NULLS)
    when(service.knownProvider(ProviderName)).thenReturn(Some(provider))
    when(
      service
        .codeGrantAccessToken(provider, s"https://$Host$CallbackUrlPath", Code)
    ).thenReturn(Future.successful(tokenResponse))
    when(service.createOrUpdateCredentials(state, tokenResponse))
      .thenReturn(Future.successful(Either.right[String, OAuth2Credential](cred)))

    val mapper = mock[TOAuth2StateMapper](RETURNS_SMART_NULLS)
    when(
      mapper.create(
        Matchers.any[OAuth2Provider],
        Matchers.eq(None),
        Matchers.eq(None),
        Matchers.eq(None),
        Matchers.eq(None)
      )
    ).thenReturn(state)
    when(mapper.getById(state.id)).thenReturn(Some(state))
    val controller =
      new OAuth2ClientController(
        CallbackUrlPath,
        CookieName,
        mapper,
        service,
        stubControllerComponents()
      )

    val authRedirect: Future[Result] =
      controller.auth(ProviderName).apply(FakeRequest())
    "set a cookie prior to redirecting to the authorization url" in {
      authRedirect.fValue.newCookies.find(_.name == CookieName) mustBe 'defined
    }

    "redirect the user to their authorization url" in {
      status(authRedirect) mustEqual 302
      header("Location", authRedirect).value must include(provider.authUrl.get)
    }

    "redirect users to their oauth bind url after receiving credentials" in {
      val callbackRequest = FakeRequest("GET", s"$CallbackUrlPath?code=$Code")
        .withHeaders("Host" -> Host)
        .withCookies(Cookie(CookieName, State))
      val bindRedirect =
        controller.callback(ProviderName).apply(callbackRequest)
      status(bindRedirect) mustEqual 303
      header("Location", bindRedirect).get must startWith(BindUrl)
    }

    "redirect users to their oauth bind url, passing back a provided error message" in {
      val errorRequest =
        FakeRequest("GET", s"$CallbackUrlPath?error=access_denied")
          .withCookies(Cookie(CookieName, State))
          .withHeaders("Host" -> Host)
      val errorResponse = controller.callback(ProviderName).apply(errorRequest)
      status(errorResponse) mustEqual 303
      header("Location", errorResponse).get must startWith(BindUrl)
      header("Location", errorResponse).get must include("action=DELETE")
      header("Location", errorResponse).get must include(
        "error=INVALID_CALLBACK"
      )
      header("Location", errorResponse).get must include(
        "message=access_denied"
      )
    }
    "redirect users to their oauth bind url, passing back a default error message" in {
      val errorRequest = FakeRequest("GET", s"$CallbackUrlPath")
        .withCookies(Cookie(CookieName, State))
        .withHeaders("Host" -> Host)
      val errorResponse = controller.callback(ProviderName).apply(errorRequest)
      status(errorResponse) mustEqual 303
      header("Location", errorResponse).get must startWith(BindUrl)
      header("Location", errorResponse).get must include("action=DELETE")
      header("Location", errorResponse).get must include(
        "error=INVALID_CALLBACK"
      )
      header("Location", errorResponse).get must include(
        "message=required+code+parameter"
      )
    }
  }

  "configured to maintain state with a cookie with no bindurl" should {
    val providerFlags = Seq(RequiresStateCookie)
    val provider = DefaultOAuth2Provider(
      ProviderName,
      Option("cid1"),
      Option("csc1"),
      authUrl = Option(s"https://$RemoteHost/auth/$ProviderName"),
      tokenUrl = Option(s"https://$RemoteHost/auth/$ProviderName/token"),
      flags = providerFlags
    )
    val tokenResponse = OAuth2TokenResponse(AccessToken)
    val state = OAuth2State(State, ProviderName, bindUrl = None)
    val cred = OAuth2Credential(ProviderName, AccessToken)

    val service = mock[OAuth2Service](RETURNS_SMART_NULLS)
    when(service.knownProvider(ProviderName)).thenReturn(Some(provider))
    when(
      service
        .codeGrantAccessToken(provider, s"https://$Host$CallbackUrlPath", Code)
    ).thenReturn(Future.successful(tokenResponse))
    when(service.createOrUpdateCredentials(state, tokenResponse))
      .thenReturn(Future.successful(Either.right(cred)))

    val mapper = mock[TOAuth2StateMapper](RETURNS_SMART_NULLS)
    when(
      mapper.create(
        Matchers.any[OAuth2Provider],
        Matchers.eq(None),
        Matchers.eq(None),
        Matchers.eq(None),
        Matchers.eq(None)
      )
    ).thenReturn(state)
    when(mapper.getById(state.id)).thenReturn(Some(state))
    val controller =
      new OAuth2ClientController(
        CallbackUrlPath,
        CookieName,
        mapper,
        service,
        stubControllerComponents()
      )
    val redirect =
      controller.auth(ProviderName).apply(FakeRequest()).futureValue

    "set a cookie prior to redirecting to the authorization url" in {
      redirect.newCookies.map(_.name) must contain(CookieName)
    }

    "redirect the user to their authorization url" in {
      redirect.header.status mustEqual 302
      redirect.header.headers("Location") must include(provider.authUrl.get)
    }

    "return the newly acquired credentials" in {
      val callbackRequest =
        FakeRequest("GET", s"$CallbackUrlPath?state=$State&code=$Code")
          .withHeaders("Host" -> Host)
      val bindRedirect =
        controller.callback(ProviderName).apply(callbackRequest)
      contentAsString(bindRedirect) mustEqual cred.asJson.noSpaces
      status(bindRedirect) mustEqual 200
    }
  }
}
