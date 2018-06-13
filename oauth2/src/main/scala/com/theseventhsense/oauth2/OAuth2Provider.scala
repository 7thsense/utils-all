package com.theseventhsense.oauth2

import cats.implicits._
import com.github.easel.auth0.JWTAuthorizer
import com.theseventhsense.utils.logging.Logging
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser
import play.api.libs.ws.WSResponse

sealed trait OAuth2AccessType {
  def name: String
}

case object Offline extends OAuth2AccessType with OAuth2Scope {
  val name = "offline"
}

case object Online extends OAuth2AccessType {
  val name = "online"
}

trait OAuth2Scope {
  def name: String

  override def toString: String = name
}

case class CustomScope(name: String) extends OAuth2Scope

object OAuth2Scope {
  val all = Seq(Offline, OpenId, Profile)

  def apply(name: String): Option[OAuth2Scope] = {
    all.find(_.name == name)
  }

  def unapply(scope: OAuth2Scope): String = {
    scope.name
  }

  case object OpenId extends OAuth2Scope {
    override val name = "openid"
  }
  case object Profile extends OAuth2Scope {
    override val name = "profile"
  }
  case object Email extends OAuth2Scope {
    override val name = "email"
  }
}

//case object User
sealed abstract class OAuth2Provider extends Product with Serializable {
  def name: String

  def clientId: Option[String]

  def clientSecret: Option[String]

  def authUrl: Option[String]

  def tokenUrl: Option[String]

  def redirectUri: Option[String]

  def authParams: Seq[AuthParam]

  def grantTypes: Seq[GrantType]

  def scopes: Seq[OAuth2Scope]

  def flags: Seq[OAuth2ProviderFlag]

  def refreshTokenInfoUrl: Option[String => String] = None

  def refreshTokenInfoParser: Option[String => Map[String, String]] = None

  def idTokenExtractor: Option[String => Map[String, String]] = None

  def accessTokenInfoUrl: Option[String => String] = None

  def additionalTokenRequestParams: Seq[(String, String)] = Seq.empty

  def requireAuthUrl(): Unit = require(
    authUrl.isDefined,
    s"Provider configuration error for $name: no auth url defined"
  )

  def requireTokenUrl(): Unit = require(
    tokenUrl.isDefined,
    s"Provider configuration error for $name: no token url defined"
  )

  def requireClientId(): Unit = require(
    clientId.isDefined,
    s"Provider configuration error for $name: no client id"
  )

  def requireClientSecret(): Unit = require(
    clientSecret.isDefined,
    s"Provider configuration error for $name: no client secret"
  )

  def getOrComputeRedirectUri(uri: String): String = {
    redirectUri.getOrElse(uri)
  }

  def withOverride(oauth2Override: OAuth2CredentialsOverride): OAuth2Provider

  def responseHandler: OAuth2ResponseHandler = OAuth2ResponseHandler.Default

}

object OAuth2Provider {
  private val jwtAuthorizer = new JWTAuthorizer()

  case class DefaultOAuth2Provider(
    name: String,
    clientId: Option[String],
    clientSecret: Option[String],
    authUrl: Option[String],
    authParams: Seq[AuthParam] = Seq.empty,
    tokenUrl: Option[String] = None,
    redirectUri: Option[String] = None,
    grantTypes: Seq[GrantType] = Seq(GrantType.Code),
    scopes: Seq[OAuth2Scope] = Seq.empty,
    flags: Seq[OAuth2ProviderFlag] = Seq.empty,
    override val idTokenExtractor: Option[String => Map[String, String]] = None,
    override val refreshTokenInfoUrl: Option[String => String] = None,
    override val refreshTokenInfoParser: Option[String => Map[String, String]] =
      None,
    override val accessTokenInfoUrl: Option[String => String] = None
  ) extends OAuth2Provider {
    def withOverride(o: OAuth2CredentialsOverride): DefaultOAuth2Provider =
      copy(
        clientId = Option(o.clientId),
        clientSecret = Option(o.clientSecret),
        authUrl = o.authUrl,
        tokenUrl = o.tokenUrl
      )

  }

  object ActOn {
    def provider(clientId: String,
                 clientSecret: String,
                 redirectUri: Option[String] = None,
                 name: String = "acton",
                 scopes: Seq[OAuth2Scope] = ActOnScope.all): OAuth2Provider = {
      DefaultOAuth2Provider(
        clientId = Option(clientId),
        clientSecret = Option(clientSecret),
        name = name,
        scopes = scopes,
        authUrl = Option("https://restapi.actonsoftware.com/authorize"),
        tokenUrl = Option("https://restapi.actonsoftware.com/token"),
        redirectUri = redirectUri,
        grantTypes = Seq(GrantType.Code, GrantType.Password),
        flags = Seq(RequiresClientCredentialsInTokenPayload)
      )
    }
  }

  object ActOnScope {
    def all: Seq[OAuth2Scope] = Seq.empty
  }
  object Eloqua {
    def provider(clientId: String,
                 clientSecret: String,
                 name: String = "eloqua"): OAuth2Provider = {
      DefaultOAuth2Provider(
        clientId = Option(clientId),
        clientSecret = Option(clientSecret),
        name = name,
        authUrl = Option("https://login.eloqua.com/auth/oauth2/authorize"),
        tokenUrl = Option("https://login.eloqua.com/auth/oauth2/token")
      )
    }
  }

  object Github {
    def provider(clientId: String,
                 clientSecret: String,
                 scopes: Seq[OAuth2Scope] = GithubScope.all,
                 name: String = "github"): OAuth2Provider = {
      DefaultOAuth2Provider(
        name = name,
        clientId = Option(clientId),
        clientSecret = Option(clientSecret),
        scopes = scopes,
        authUrl = Some("https://github.com/login/oauth/authorize"),
        tokenUrl = Some("https://github.com/login/oauth/access_token"),
        flags = Seq(RequiresClientCredentialsInTokenPayload, RefreshModeNone)
      )
    }
  }

  object GithubScope {
    val all = Seq(User, UserEmail)

    case object User extends OAuth2Scope {
      val name = "user"
    }

    case object UserEmail extends OAuth2Scope {
      val name = "user:email"
    }

  }

  object Google {
    def jwtExtractor(jwt: String): Map[String, String] =
      jwtAuthorizer.verify(jwt) match {
        case Left(_) => Map.empty
        case Right(idToken) =>
          idToken.claims.flatMap {
            case (k, v) =>
              Option(v.asBoolean)
                .map(_.toString)
                .orElse(Option(v.asString))
                .map(optV => (k, optV))
          }
      }

    def provider(clientId: String,
                 clientSecret: String,
                 scopes: Seq[OAuth2Scope] = GoogleScope.imapWithEmail,
                 name: String = "google"): OAuth2Provider = {
      DefaultOAuth2Provider(
        name = name,
        scopes = scopes,
        clientId = Option(clientId),
        clientSecret = Option(clientSecret),
        authUrl = Some("https://accounts.google.com/o/oauth2/auth"),
        tokenUrl = Some("https://accounts.google.com/o/oauth2/token"),
        idTokenExtractor = Some(jwtExtractor)
      )
    }
  }

  object GoogleScope {
    val all = Seq(
      CalendarRO,
      Calendar,
      Profile,
      Email,
      Mail,
      GmailModify,
      GmailReadOnly,
      GmailMetadata
    )
    val imapOnly = Seq(Mail)
    val imapWithEmail = Seq(Mail, OAuth2Scope.Email)
    case object CalendarRO extends OAuth2Scope {
      val name = "https://www.googleapis.com/auth/calendar.readonly"
    }
    case object Calendar extends OAuth2Scope {
      val name = "https://www.googleapis.com/auth/calendar"
    }
    case object Profile extends OAuth2Scope {
      val name = "https://www.googleapis.com/auth/userinfo.profile"
    }
    case object Email extends OAuth2Scope {
      val name = "https://www.googleapis.com/auth/userinfo.email"
    }
    case object Mail extends OAuth2Scope {
      val name = "https://mail.google.com/"
    }
    case object GmailModify extends OAuth2Scope {
      val name = "https://www.googleapis.com/auth/gmail.modify"
    }
    case object GmailReadOnly extends OAuth2Scope {
      val name = "https://www.googleapis.com/auth/gmail.readonly"
    }
    case object GmailMetadata extends OAuth2Scope {
      val name = "https://www.googleapis.com/auth/gmail.metadata"
    }
  }

  object HubspotOAuth1 {
    def provider(clientId: String,
                 clientSecret: String,
                 scopes: Seq[OAuth2Scope] = HubspotOAuth1Scope.all,
                 name: String = Name): OAuth2Provider = {
      DefaultOAuth2Provider(
        name = name,
        clientId = Option(clientId),
        clientSecret = Option(clientSecret),
        scopes = scopes,
        authUrl = Some("https://app.hubspot.com/auth/authenticate"),
        tokenUrl = Some("https://api.hubapi.com/auth/v1/refresh"),
        authParams = Seq(AuthParamPortalId),
        flags = Seq(
          RefreshModeScope,
          RequiresStateCookie,
          RequiresClientIdInTokenPayload,
          SkipsTokenRequest,
          AuthorizationMechanismFlag.TokenAsQueryParameter
        )
      )
    }

    case object AuthParamPortalId extends AuthParam {
      val name = "portalId"
    }

    val Name = "hubspot"
  }

  object HubspotOAuth1Scope {
    val all = Seq(EventsRW)
    case object EventsRW extends OAuth2Scope {
      val name = "events-rw"
    }
  }

  object HubspotOAuth2 {

    /**
      * https://developers.hubspot.com/docs/methods/oauth2/get-refresh-token-information
      *
      * Parse the hubspot refresh token info into an array suitable for passing along as
      * query string parameters. This is used by the oauth2 binding process to pass along
      * additional metadata to the application.
      *
      * @param info
      * @return
      */
    def refreshTokenInfoParser(info: String): Map[String, String] =
      (for {
        refreshTokenInfoJson <- parser.parse(info).toOption
        obj <- refreshTokenInfoJson.asObject

      } yield
        Seq(
          obj("hub_id").flatMap(_.asNumber).map("hub_id" -> _.toString),
          obj("hub_domain").flatMap(_.asString).map("hub_domain" -> _),
          obj("user").flatMap(_.asString).map("user" -> _),
          obj("user_id").flatMap(_.asNumber).map("user_id" -> _.toString)
        ).flatten.toMap).getOrElse(Map.empty)

    def provider(clientId: String,
                 clientSecret: String,
                 scopes: Seq[OAuth2Scope] = HubspotOAuth2Scope.all,
                 name: String = Name): OAuth2Provider = {
      DefaultOAuth2Provider(
        name = name,
        clientId = Option(clientId),
        clientSecret = Option(clientSecret),
        scopes = scopes,
        authUrl = Some("https://app.hubspot.com/oauth/authorize"),
        tokenUrl = Some("https://api.hubapi.com/oauth/v1/token"),
        flags =
          Seq(RequiresStateCookie, RequiresClientCredentialsInTokenPayload),
        refreshTokenInfoUrl = Some(
          (token: String) =>
            s"https://api.hubapi.com/oauth/v1/refresh-tokens/$token"
        ),
        refreshTokenInfoParser = Some(refreshTokenInfoParser),
        accessTokenInfoUrl = Some(
          (token: String) =>
            s"https://api.hubapi.com/oauth/v1/access-tokens/$token"
        )
      )
    }

    val Name: String = "hubspot-oauth2"
  }

  object HubspotOAuth2Scope {
    val all = Seq(Automation, Contacts, Content)
    case object Automation extends OAuth2Scope {
      val name = "automation"
    }
    case object Contacts extends OAuth2Scope {
      val name = "contacts"
    }
    case object Content extends OAuth2Scope {
      val name = "content"
    }
  }

  object LinkedIn {
    def provider(clientId: String,
                 clientSecret: String,
                 scopes: Seq[OAuth2Scope] = LinkedInScope.all,
                 name: String = "linkedin"): OAuth2Provider = {
      DefaultOAuth2Provider(
        name = name,
        clientId = Option(clientId),
        clientSecret = Option(clientSecret),
        scopes = scopes,
        authUrl = Some("https://www.linkedin.com/uas/oauth2/authorization"),
        tokenUrl = Some("https://www.linkedin.com/uas/oauth2/accessToken"),
        flags = Seq(RefreshModeNone)
      )
    }
  }

  object LinkedInScope {
    val all = Seq(FullProfileR, EmailR)
    case object FullProfileR extends OAuth2Scope {
      val name = "r_fullprofile"
    }
    case object EmailR extends OAuth2Scope {
      val name = "r_emailaddress"
    }
  }

  object Marketo extends Logging {
    def baseProvider(partnerApiKey: Option[String]): OAuth2Provider =
      Provider(
        additionalTokenRequestParams =
          partnerApiKey.toSeq.map(key => "partner_id" -> key)
      )

    private case class ErrorMessage(code: String, message: String)
    private case class ErrorResponse(requestId: String,
                                     success: Boolean,
                                     errors: Seq[ErrorMessage])
    private implicit val errorMessageDecoder = deriveDecoder[ErrorMessage]
    private implicit val errorResponseDecoder = deriveDecoder[ErrorResponse]

    private val RefreshableErrorCodes = Set(
      "601", // Access token invalid
      "602" // Access token expired
    )

    private val RetriableErrorCodes = Set(
      "601", // Access token invalid
      "602", // Access token expired
      "604", // Request timeed out
      "606", // Max rate limit exceeded
      "608", // API temporarily unavailable
      "615" // Concurrent access limit reached
    )

    def shouldRefreshViaJsonBody(body: String): Boolean =
      parser.decode[ErrorResponse](body) match {
        case Left(_) =>
          false
        case Right(ErrorResponse(_, _, errors)) =>
          errors.headOption match {
            case Some(ErrorMessage(code, _)) if RefreshableErrorCodes.contains(code) =>
              true
            case _ =>
              false
          }
      }

    def tokenUrlFromIdentityEndpoint(identityEndpointUri: String): String =
      s"$identityEndpointUri/oauth/token"

    case class Provider(
      override val clientId: Option[String] = None,
      override val clientSecret: Option[String] = None,
      override val authUrl: Option[String] = None,
      override val tokenUrl: Option[String] = None,
      override val additionalTokenRequestParams: Seq[(String, String)] =
        Seq.empty
    ) extends OAuth2Provider {
      def name: String = "marketo"
      def authParams: Seq[AuthParam] = AuthParam.ClientCredentialsParams
      def redirectUri: Option[String] = None
      def grantTypes: Seq[GrantType] = Seq(GrantType.Code)
      def scopes: Seq[OAuth2Scope] = MarketoScope.all.toSeq
      def flags: Seq[OAuth2ProviderFlag] =
        Seq(RequiresClientCredentialsGrantType, RefreshModeClientCredentials)

      override def withOverride(o: OAuth2CredentialsOverride): Provider = copy(
        clientId = Option(o.clientId),
        clientSecret = Option(o.clientSecret),
        authUrl = None,
        tokenUrl = o.tokenUrl
      )

      override def responseHandler: OAuth2ResponseHandler = ResponseHandler
    }

    object ResponseHandler extends OAuth2ResponseHandler.Http401 {
      override def shouldRefresh(response: WSResponse): Boolean = {
        super.shouldRefresh(response) || shouldRefreshViaJsonBody(response.body)
      }
    }
  }

  object MarketoScope {
    val all: Set[OAuth2Scope] = Set.empty
  }

  object Office365 {
    def provider(clientId: String,
                 clientSecret: String,
                 name: String = "office365",
                 scopes: Seq[OAuth2Scope] = Seq.empty): OAuth2Provider = {
      DefaultOAuth2Provider(
        clientId = Option(clientId),
        clientSecret = Option(clientSecret),
        name = name,
        scopes = scopes,
        authUrl = Some("https://login.windows.net/common/oauth2/authorize"),
        tokenUrl = Some("https://login.windows.net/common/oauth2/token")
      )
    }

  }

  object Salesforce {
    def provider(clientId: String,
                 clientSecret: String,
                 scopes: Set[OAuth2Scope] = SalesforceScope.all,
                 name: String = "salesforce"): OAuth2Provider = {
      DefaultOAuth2Provider(
        name = name,
        clientId = Option(clientId),
        clientSecret = Option(clientSecret),
        authUrl = Some("https://login.salesforce.com/services/oauth2/authorize"),
        tokenUrl = Some("https://login.salesforce.com/services/oauth2/token"),
        flags = Seq(RequiresClientCredentialsInTokenPayload)
      )
    }
  }

  object SalesforceScope {
    val all: Set[OAuth2Scope] = Set.empty
  }
}
