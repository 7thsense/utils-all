package com.theseventhsense.oauth2

import com.theseventhsense.oauth2.OAuth2Provider._
import com.theseventhsense.testing.{AkkaUnitSpec, UnitSpec, WSClientFactory}
import com.theseventhsense.testing.tags.ExternalAPI
import com.theseventhsense.utils.oauth2.models.OAuth2State
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

trait ProviderSpec extends AkkaUnitSpec with WSClientFactory with ScalaFutures {
  def oauth2App: OAuth2Provider

  def username: String

  def password: String

  implicit val persistence = new InMemoryOAuth2Persistence()

  s"the ${OAuth2Service.getClass.getSimpleName}" should {
    "be able to login" taggedAs ExternalAPI in {
      val response = OAuth2Service
        .passwordGrantAccessToken(oauth2App, username, password)
        .futureValue
      response.accessToken must not equal ""
    }
  }
}

trait SalesforceOAuth2ServiceSpec extends AkkaUnitSpec {
  val stateMapper = new MemoryStateMapper()
  val consumerKey =
    "3MVG9fMtCkV6eLheWYI53C7Yz6VIquwpDeOmoOLsVloD_.R0176NWr.20Npy2t7iQ4JqxBLiedtVmb6dMk7KG"
  val consumerSecret = "SECRET"
  val oauth2App = DefaultOAuth2Provider(
    name = "salesforce",
    clientId = Option(consumerKey),
    clientSecret = Option(consumerSecret),
    authUrl = Some("https://login.salesforce.com/services/oauth2/authorize"),
    tokenUrl = Some("https://login.salesforce.com/services/oauth2/token"),
    flags = Seq(RequiresClientCredentialsInTokenPayload)
  )

  "the salesforce provider" should {
    "be able to construct an authorization url" ignore {
      val url =
        OAuth2Service.authRequestURL(
          oauth2App,
          "https://localhost:9443",
          OAuth2State("provider1", "salesforce"),
          offline = true
        )
      url mustEqual "https://login.salesforce.com/services/oauth2/authorize" +
        "?state=state1" +
        "&scope=&client_id=3MVG9fMtCkV6eLheWYI53C7Yz6VIquwpDeOmoOLsVloD_.R0176NWr.20Npy2t7iQ4JqxBLiedtVmb6dMk7KG" +
        "&redirect_uri=https%3A%2F%2Flocalhost%3A9443" +
        "&access_type=offline" +
        "&approval_prompt=auto" +
        "&response_type=code"
    }
  }
}

class MarketoOAuth2ServiceSpec extends AkkaUnitSpec with WSClientFactory {
  val stateMapper = new MemoryStateMapper()
  val clientId = "84b0086a-39e9-473b-9ddf-ee680f526686"
  val clientSecret = "SECRET"
  val identityEndpointUri = "https://485-BLA-793.mktorest.com/identity"
  val oAuth2CredentialsOverride = OAuth2CredentialsOverride(
    clientId,
    clientSecret,
    None,
    Option(Marketo.tokenUrlFromIdentityEndpoint(identityEndpointUri))
  )
  val oauth2Provider =
    Marketo.baseProvider(None).withOverride(oAuth2CredentialsOverride)

  "the marketo provider" should {
    "be able to construct a token url" in {
      oauth2Provider.tokenUrl must be('defined)
    }
    "be able to construct an authorization url" ignore {
      val url =
        OAuth2Service.authRequestURL(
          oauth2Provider,
          "https://localhost:9443",
          OAuth2State("provider1", "marketo"),
          offline = true
        )
      logger.warn(url)
      url mustEqual "https://485-BLA-793.mktorest.com/identity/authorize" +
        "?state=state1" +
        s"&scope=&client_id=$clientId" +
        "&redirect_uri=https%3A%2F%2Flocalhost%3A9443" +
        "&access_type=offline" +
        "&approval_prompt=auto" +
        "&response_type=code"
    }
  }
}

class HubspotOAuth2ServiceSpec extends UnitSpec {
  val service = OAuth2Service
  val oauth2App = DefaultOAuth2Provider(
    name = "hubspot",
    clientId = Option("CLIENT_ID"),
    clientSecret = Option("SECRET"),
    authUrl = Option("https://app.hubspot.com/auth/authenticate"),
    scopes = Seq(CustomScope("events-rw")),
    flags = Seq(RefreshModeScope, RequiresStateCookie)
  )

  "the hubspot app" should {
    "be able to construct a url" in {
      val url =
        service.authRequestURL(
          oauth2App,
          "https://localhost:9443",
          OAuth2State("id1", "salesforce"),
          offline = true,
          extraParams = Map("portalId" -> "500769")
        )
      url mustEqual "https://app.hubspot.com/auth/authenticate" +
        "?portalId=500769" +
        "&redirect_uri=https%3A%2F%2Flocalhost%3A9443" +
        "&approval_prompt=auto" +
        "&response_type=code" +
        "&client_id=CLIENT_ID" +
        "&scope=events-rw+offline"
    }
  }
}

class GoogleOAuth2ServiceSpec extends UnitSpec {
  val service = OAuth2Service
  val oauth2App = DefaultOAuth2Provider(
    name = "google",
    clientId = Option("CLIENT_ID"),
    clientSecret = Option("SECRET"),
    authUrl = Option("https://accounts.google.com/o/oauth2/auth"),
    tokenUrl = Option("https://accounts.google.com/o/oauth2/token"),
    scopes = Seq(
      "https://www.googleapis.com/auth/calendar.readonly",
      "https://www.googleapis.com/auth/calendar",
      "https://www.googleapis.com/auth/userinfo.profile",
      "https://www.googleapis.com/auth/userinfo.email",
      "https://mail.google.com/"
    ).map(CustomScope)
  )

  "the google app" should {
    "be able to construct an auth url" ignore {
      val state = OAuth2State("1234-abc3", "google")
      val url = service.authRequestURL(
        oauth2App,
        "https://localhost:9443",
        state,
        offline = true
      )
      url mustEqual "https://accounts.google.com/o/oauth2/auth" +
        "?redirect_uri=https%3A%2F%2Flocalhost%3A9443" +
        "&state=1234-abc3" +
        "&client_id=141429731361-gpist5o8fif3hca83hkct3kdf2e6gntj.apps.googleusercontent.com" +
        "&access_type=offline" +
        "&scope=https%3A%2F%2Fmail.google.com%2F+" +
        "https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fcalendar+" +
        "https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fcalendar.readonly+" +
        "https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.email+" +
        "https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.profile" +
        "&approval_prompt=auto" +
        "&response_type=code"
    }
  }
}

class LinkedInOAuth2ServiceSpec extends UnitSpec {
  val service = OAuth2Service
  val oauth2App = DefaultOAuth2Provider(
    name = "linkedin",
    clientId = Option("refl7s4uv7j6"),
    clientSecret = Option("SECRET"),
    authUrl = Option("https://www.linkedin.com/uas/oauth2/authorization"),
    scopes = Seq("r_fullprofile", "r_emailaddress")
      .map(CustomScope),
    flags = Seq(RefreshModeNone)
  )

  "the linkedin configuration" should {
    "be able to construct an auth url" ignore {
      val url = service.authRequestURL(
        oauth2App,
        "https://localhost:9443",
        OAuth2State("abcd-1234", "linkedin")
      )
      url mustEqual "https://www.linkedin.com/uas/oauth2/authorization" +
        "?state=abcd-1234" +
        "&redirect_uri=https%3A%2F%2Flocalhost%3A9443" +
        "&client_id=refl7s4uv7j6" +
        "&scope=r_emailaddress+r_fullprofile" +
        "&approval_prompt=auto" +
        "&response_type=code"
    }
  }
}

trait ActOnOAuth2ServiceSpec extends UnitSpec {
  val service = OAuth2Service
  val oauth2App = ActOn.provider(
    name = "acton",
    clientId = "2HOu3YpsjJUZc3qDkOqW3dXAYEIa",
    clientSecret = "SECRET"
  )

  "the acton configuration" should {
    "be able to construct an auth url" in {
      val url = service.authRequestURL(
        oauth2App,
        "https://localhost:9443/oauth2/client/acton/callback",
        OAuth2State("abcd-1234", "acton")
      )
      url mustEqual ""
    }
  }
}

// Twitter doesn't yet support Oauth2 for user auth
class TwitterOAuth2ServiceSpec extends UnitSpec {
  val service = OAuth2Service
  val oauth2App = DefaultOAuth2Provider(
    name = "twitter",
    clientId = Option("ke55PesYtC4sdTv8LakxAIk5a"),
    clientSecret = Option("SECRET"),
    authUrl = None
  )
}
