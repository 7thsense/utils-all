package com.theseventhsense.oauth2

import java.net.URLEncoder

import scala.collection.concurrent
import scala.collection.immutable.ListMap
import scala.concurrent.{ExecutionContext, Future}

import cats.data.OptionT
import cats.implicits._
import com.typesafe.scalalogging.Logger
import io.circe.parser
import javax.inject.Inject
import play.api.libs.ws.{StandaloneWSClient, StandaloneWSResponse, WSAuthScheme}
import play.api.libs.ws.DefaultBodyWritables._

import com.theseventhsense.oauth2.OAuth2Codecs._
import com.theseventhsense.utils.Verifications
import com.theseventhsense.utils.cats._
import com.theseventhsense.utils.cats.syntax._
import com.theseventhsense.utils.logging.{LogContext, Logging}
import com.theseventhsense.utils.models.TLogContext
import com.theseventhsense.utils.oauth2.models.OAuth2State
import com.theseventhsense.utils.persistence.AkkaMessage
import com.theseventhsense.utils.types.SSDateTime

object OAuth2Service extends Logging {
  import com.theseventhsense.clients.wsclient.WireLogging._

  sealed abstract class Error extends Throwable with Product with AkkaMessage
  sealed abstract class UnrecoverableError extends Error
  case class BadRequestError(response: String) extends UnrecoverableError {
    override def getMessage: String = s"Bad request, got $response"
  }
  case class RefreshError(t: Throwable) extends Error {
    override def getMessage: String = t.getMessage
  }
  case class DecodeError(error: io.circe.Error) extends Error {
    override def getMessage: String = error.getMessage
  }
  case class UnknownError(throwable: Throwable) extends Error {
    override def getMessage: String = throwable.getMessage
  }

  /**
    * Construct the url to send a user to to get an authorization code. The user
    * must be taken to this url in their browser, causing the application to "lose"
    * the users context. The user will be redirected back to @callbackUrl by the
    * oauth provider, where the @state value can be used to tie the response
    * back to the request. Upon completion it should return an authorization
    * grant, specifically an "authorization code" grant.
    *
    * Implements the "Authorization Request" step "A" of the Oauth 2.0 flow
    * documented in RFC 6749.
    *
    * @param provider
    * @param callbackUrl
    * @param extraParams
    * @param forceApproval
    * @param loginHint
    * @param state
    * @return
    */
  def authRequestURL(
    provider: OAuth2Provider,
    callbackUrl: String,
    state: OAuth2State,
    offline: Boolean = false,
    extraParams: Map[String, String] = Map.empty,
    forceApproval: Boolean = false,
    loginHint: Option[String] = None,
    next: Option[String] = None
  )(implicit logContext: LogContext): String = {
    require(
      provider.authUrl.isDefined,
      s"Provider configuration error for ${provider.name}: missing auth url"
    )
    require(
      provider.clientId.isDefined,
      s"Provider configuration error for ${provider.name}: missing clientId"
    )
    val approvalPrompt = if (forceApproval) "force" else "auto"
    val stateOpt =
      if (provider.flags.contains(RequiresStateCookie)) None else Some(state)
    val accessType =
      if (offline && !provider.flags.contains(RefreshModeScope))
        Some("offline")
      else None
    val scopes = if (offline && provider.flags.contains(RefreshModeScope)) {
      provider.scopes ++ Set(Offline)
    } else {
      provider.scopes
    }
    val optionalParams: Map[String, String] =
      Map(
        "state" -> stateOpt.map(_.id),
        "login_hint" -> loginHint,
        "access_type" -> accessType
      ).filter(_._2.isDefined).mapValues(_.get)
    val queryParams: Map[String, String] = Map(
      "response_type" -> ResponseType.Code.toString,
      "client_id" -> provider.clientId.get,
      "scope" -> scopes.map(_.toString).sorted.mkString(" "),
      "redirect_uri" -> callbackUrl,
      "approval_prompt" -> approvalPrompt
    ) ++ optionalParams ++ extraParams
    val sortedParams = ListMap(queryParams.toSeq.sortBy(_._1): _*)
    logger.trace(s"Returning params for ${provider.name}: $sortedParams")
    val url = provider.authUrl.get + encode(sortedParams)
    url
  }

  /**
    * Execute a request to get an access token.
    *
    * @param provider
    * @param payload
    * @param client
    * @return
    */
  private def accessTokenRequest(provider: OAuth2Provider,
                                 payload: Map[String, Seq[String]])(
    implicit client: StandaloneWSClient,
    ec: ExecutionContext,
    logContext: LogContext
  ): Future[StandaloneWSResponse] = {
    Verifications.verify(
      provider.clientId.isDefined,
      s"Provider configuration error for ${provider.name}: missing client id"
    )
    Verifications.verify(
      provider.clientSecret.isDefined,
      s"Provider configuration error for ${provider.name}: missing client secret"
    )
    Verifications.verify(
      provider.tokenUrl.isDefined,
      s"Provider configuration error for ${provider.name}: missing token url"
    )
    val requiresClientIdInPayload = provider.flags.contains(
      RequiresClientCredentialsInTokenPayload
    ) ||
      provider.flags.contains(RequiresClientIdInTokenPayload)

    // If the provider requires the client id in the payload, add it.
    val localPayload = if (requiresClientIdInPayload) {
      val secretPayload =
        if (provider.flags.contains(RequiresClientCredentialsInTokenPayload)) {
          Map("client_secret" -> Seq(provider.clientSecret.get))
        } else {
          Seq.empty
        }
      payload ++ secretPayload ++ Map("client_id" -> Seq(provider.clientId.get))
    } else {
      payload
    }
    var request = client
      .url(provider.tokenUrl.get)
      .withFollowRedirects(follow = false)
      .addHttpHeaders("Accept" -> "application/json")
      .addHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded")

    // Since we aren't providing the client ID in the payload, add the client
    // id and secret as http basic parameters per RFC 6749 4.1.3
    if (!requiresClientIdInPayload) {
      request = request.withAuth(
        provider.clientId.get,
        provider.clientSecret.get,
        WSAuthScheme.BASIC
      )
    }

    logger.debug(
      "Requesting access token from " + request.url + " payload:" + localPayload
    )
    request.withOptionalWireLogging().post(localPayload)
  }

  /**
    * Construct a credentials object from a state and a token response.
    *
    * @param oauth2State
    * @param response
    * @return
    */
  private def credentialsFromResponse(
    oauth2State: OAuth2State,
    response: OAuth2TokenResponse
  )(implicit logContext: LogContext): OAuth2Credential = {
    val creds = OAuth2Credential(
      id = oauth2State.oAuth2Id.getOrElse(OAuth2Id.Default),
      providerName = oauth2State.providerName,
      accessToken = response.access_token,
      accessExpires = response.accessExpires,
      refreshToken = response.refresh_token
    )
    oauth2State.oAuth2Override match {
      case None => creds
      case Some(o) =>
        creds.copy(
          authUrl = o.authUrl,
          tokenUrl = o.tokenUrl,
          clientId = Option(o.clientId),
          clientSecret = Option(o.clientSecret)
        )
    }
  }

  private def updateCredentials(oAuth2Credential: OAuth2Credential)(
    implicit ec: ExecutionContext,
    logContext: LogContext,
    oAuth2Persistence: TOAuth2Persistence
  ): Future[OAuth2Credential] = {
    logger.debug(
      s"Saving credentials for ${oAuth2Credential.providerName}: ${oAuth2Credential.id}"
    )
    oAuth2Persistence.save(oAuth2Credential)
  }

  private def createCredentials(oauth2State: OAuth2State,
                                response: OAuth2TokenResponse)(
    implicit ec: ExecutionContext,
    logContext: LogContext,
    oAuth2Persistence: TOAuth2Persistence
  ): Future[OAuth2Credential] = {
    oAuth2Persistence
      .create(credentialsFromResponse(oauth2State, response))
      .map { creds =>
        logger.debug(
          s"Creating credentials for ${creds.providerName}: ${creds.id}"
        )
        creds
      }
  }

  private def createOrUpdateCredentials(oauth2State: OAuth2State,
                                        response: OAuth2TokenResponse)(
    implicit ec: ExecutionContext,
    logContext: LogContext,
    oAuth2Persistence: TOAuth2Persistence
  ): Future[Either[String, OAuth2Credential]] = {
    val existingCredentialsFutOpt: Future[Either[String, OAuth2Credential]] =
      oauth2State.oAuth2Id match {
        case None =>
          Future.successful(Either.left("No oauth2 credentials found"))
        case Some(id) =>
          logger.trace(
            s"Loading existing OAuth2 credentials for ${oauth2State.oAuth2Id} - $id"
          )
          oAuth2Persistence
            .get(id)
            .map(Either.fromOption(_, s"Error loading oAuth2Id $id"))
      }
    existingCredentialsFutOpt.flatMap {
      case Left(e) =>
        logger.trace(s"Creating OAuth2 credentials for ${oauth2State.oAuth2Id}")
        createCredentials(oauth2State, response).map(Either.right)
      case Right(cred: OAuth2Credential) =>
        logger.trace(s"Updating OAuth2 credentials for ${oauth2State.oAuth2Id}")
        updateCredentials(cred.update(response)).map(Either.right)
    }
  }

  private def parseAccessTokenResponse(
    result: StandaloneWSResponse
  )(implicit logContext: LogContext): Either[Error, OAuth2TokenResponse] =
    result.status match {
      case 200 =>
        val response = parser.decode[OAuth2TokenResponse](result.body)
        response match {
          case Left(e) =>
            logger.warn(
              s"Failed to parse grant token response: ${result.body}",
              e
            )
            Left(DecodeError(e))
          case Right(x) =>
            logger.trace(
              s"Parsed grant token response response: ${result.body}"
            )
            Right(x)
        }

      case 400 =>
        Left(BadRequestError(result.body))

      case x =>
        val title =
          s"Unsupported response for oauth token request: $x: ${result.statusText}"
        val description =
          s"""
             | ${result.body}
             """.stripMargin
        val e = new OAuth2Exception(title, description)
        logger.debug(title + description, e)
        Left(RefreshError(e))
    }

  /**
    * Format, post, and parse the response for an OAuth2 grant
    *
    * @param payload
    * @param client
    * @return
    */
  private def accessTokenViaPost(provider: OAuth2Provider,
                                 payload: Map[String, Seq[String]])(
    implicit client: StandaloneWSClient,
    ec: ExecutionContext,
    logContext: LogContext
  ): Future[OAuth2TokenResponse] = {
    val resultFuture = accessTokenRequest(provider, payload)
    resultFuture.map(parseAccessTokenResponse).flattenEither
  }

  /**
    * If the provider has defined a refreshTokenInfoUrl and we
    * have received a refresh token, get the refreshTokenInfo as a
    * string and call onNewRefreshToken with it.
    *
    * This allows us to get the hubspot hub id, etc back into the application.
    *
    * @param provider
    * @param client
    * @param ec
    * @return
    */
  def refreshTokenInfo(provider: OAuth2Provider, refreshToken: String)(
    implicit client: StandaloneWSClient,
    ec: ExecutionContext,
    logContext: LogContext
  ): Future[Map[String, String]] =
    (for {
      urlFunc <- provider.refreshTokenInfoUrl
      mapFunc <- provider.refreshTokenInfoParser
    } yield
      client
        .url(urlFunc(refreshToken))
        .withOptionalWireLogging()
        .execute()
        .map(_.body)
        .map(mapFunc))
      .getOrElse(Future.successful(Map.empty))

  /**
    * Request an access token from the provider via a code, typically provided via a callback. This is
    * the "standard" 3-legged oAuth flow.
    *
    * @param provider
    * @param redirectUri
    * @param code
    * @param client
    * @param ec
    * @return
    */
  private def codeGrantAccessToken(provider: OAuth2Provider,
                                   redirectUri: String,
                                   code: String)(
    implicit client: StandaloneWSClient,
    ec: ExecutionContext,
    logContext: LogContext
  ): Future[OAuth2TokenResponse] = {
    provider.requireTokenUrl()
    val payload = Map(
      "code" -> Seq(code),
      "grant_type" -> Seq(GrantType.Code.toString),
      "redirect_uri" -> Seq(provider.getOrComputeRedirectUri(redirectUri))
    )
    logger.trace(s"Constructing code grant access token request from $payload")
    accessTokenViaPost(provider, payload)
  }

  /**
    * Log directly into an oauth2 api using the "password" grant type. This
    * skips the "authorization request" phase of the oauth2 spec in favor of
    * directly submitting the users credentials.
    *
    * See https://www.salesforce.com/us/developer/docs/api_rest/
    *
    * @param provider The oauth2 application credentials
    * @param username
    * @param password
    * @param client
    * @return
    */
  def passwordGrantAccessToken(provider: OAuth2Provider,
                               username: String,
                               password: String)(
    implicit client: StandaloneWSClient,
    ec: ExecutionContext,
    logContext: LogContext
  ): Future[OAuth2Credential] = {
    require(
      provider.tokenUrl.isDefined,
      s"Provider configuration error for ${provider.name}: missing token url"
    )
    val payload = Map(
      "grant_type" -> Seq(GrantType.Password.toString),
      "username" -> Seq(username),
      "password" -> Seq(password)
    )
    logger.trace(
      s"Constructing password grant access token request with $payload"
    )
    accessTokenViaPost(provider, payload) flatMap { response =>
      val credential = OAuth2Credential.from(provider, response)
      Future.successful(credential)
    }
  }

  /**
    * Log directly into an oauth2 api using the "client_credentials" grant type. This
    * skips the "authorization request" phase of the oauth2 spec in favor of
    * directly submitting a clientId and client secret. These are assumed to be
    * unique-per-user.
    *
    * See https://www.salesforce.com/us/developer/docs/api_rest/
    *
    * @param provider The oauth2 application credentials
    * @return
    */
  private def clientCredentialsAccessToken(provider: OAuth2Provider)(
    implicit client: StandaloneWSClient,
    ec: ExecutionContext,
    logContext: LogContext
  ): Future[OAuth2TokenResponse] = {
    provider.requireClientId()
    provider.requireClientSecret()
    provider.requireTokenUrl()
    val queryParameters = Map(
      "grant_type" -> GrantType.ClientCredentials.toString,
      "client_id" -> provider.clientId.get,
      "client_secret" -> provider.clientSecret.get
    ) ++ provider.additionalTokenRequestParams
    val request = client
      .url(provider.tokenUrl.get)
      .addQueryStringParameters(queryParameters.toSeq: _*)
      .withFollowRedirects(follow = false)
      .addHttpHeaders("Accept" -> "application/json")

    logger.debug(
      s"Requesting authorization token via client credentials ${request.uri.toString}"
    )
    request
      .withOptionalWireLogging()
      .get()
      .map(parseAccessTokenResponse)
      .flattenEither
  }

  /**
    * Refresh a client credential using the appropriate method configured for the provider.
    *
    * @param provider
    * @param oAuth2Credential
    * @param client
    * @param ec
    * @param oAuth2Persistence
    * @return
    */
  private def refresh(provider: OAuth2Provider,
                      oAuth2Credential: OAuth2Credential)(
    implicit client: StandaloneWSClient,
    ec: ExecutionContext,
    logContext: LogContext,
    oAuth2Persistence: TOAuth2Persistence
  ): Future[OAuth2Credential] = {
    provider.requireTokenUrl()
    if (provider.flags.contains(RefreshModeClientCredentials)) {
      refreshViaClientCredentials(provider, oAuth2Credential)
    } else {
      refreshViaRefreshToken(provider, oAuth2Credential)
    }
  }

  /**
    * Execute a refresh using client credentials. This workflow is sometimes referred to as "2 Legged OAuth", since
    * there is no "user" step, just the provider and consumer. If client credentials exist on the oAuth2Credential
    * object, they are used, otherwise any defaults defined on the provider itself will be used.
    *
    * @param provider
    * @param oAuth2Credential
    * @param client
    * @param ec
    * @param oAuth2Persistence
    * @return
    */
  private def refreshViaClientCredentials(provider: OAuth2Provider,
                                          oAuth2Credential: OAuth2Credential)(
    implicit client: StandaloneWSClient,
    ec: ExecutionContext,
    logContext: LogContext,
    oAuth2Persistence: TOAuth2Persistence
  ): Future[OAuth2Credential] = {
    logger.debug(s"Refreshing access token request via client credentials")
    clientCredentialsAccessToken(provider)
      .flatMap(updateAccessToken(oAuth2Credential))
  }

  /**
    * Execute a refresh using a refresh token. This is typically part of the "offline" scope requested
    * as a part of a "3 Legged OAuth" connection. If the provider oAuth2Credential has no refresh token, this
    * will fail out.
    *
    * @param provider
    * @param oAuth2Credential
    * @param client
    * @param ec
    * @param oAuth2Persistence
    * @return
    */
  private def refreshViaRefreshToken(provider: OAuth2Provider,
                                     oAuth2Credential: OAuth2Credential)(
    implicit client: StandaloneWSClient,
    ec: ExecutionContext,
    logContext: LogContext,
    oAuth2Persistence: TOAuth2Persistence
  ): Future[OAuth2Credential] = {
    oAuth2Credential.requireRefreshToken()
    val payload = Map(
      "refresh_token" -> Seq(oAuth2Credential.refreshToken.get),
      "grant_type" -> Seq(GrantType.RefreshToken.toString)
    )
    logger.trace(s"Constructing refresh access token request from $payload")
    accessTokenViaPost(provider, payload)
      .flatMap(updateAccessToken(oAuth2Credential))
  }

  /**
    * Save an updated access token vai the persistence mechanism.
    *
    * @param oAuth2Credential
    * @param response
    * @param oAuth2Persistence
    * @return
    */
  private def updateAccessToken(oAuth2Credential: OAuth2Credential)(
    response: OAuth2TokenResponse
  )(implicit ec: ExecutionContext,
    logContext: LogContext,
    oAuth2Persistence: TOAuth2Persistence): Future[OAuth2Credential] = {
    val credential = oAuth2Credential.update(response)
    oAuth2Persistence.save(credential)
  }

  def encode(s: String): String =
    URLEncoder.encode(s, "UTF-8")

  def encode(params: Map[String, Any]): String = {
    val encoded = for {
      (name, value) <- params if value != None
      encodedValue = value match {
        case Some(x) => encode(x.toString)
        case x       => encode(x.toString)
      }
    } yield name + "=" + encodedValue

    encoded.mkString("?", "&", "")
  }

  /**
    * A map of outstanding refresh requests. This prevents dogpiling the access server,
    * instead blocking all the clients and handing them the first future to be requested.
    */
  private val refreshFutures
    : concurrent.Map[OAuth2Id, Future[OAuth2Credential]] =
    concurrent.TrieMap.empty

}

/**
  * The stateful, configured interface to the oAuth2 module. Generally, the providers and persistence
  * would be injected via DI. Where possible, implementations in this class simply delegate to the (more pure)
  * functions defined in the companion object.
  *
  * @param providers
  * @param oAuth2Persistence
  */
@javax.inject.Singleton
class OAuth2Service @Inject()(providers: Set[OAuth2Provider])(
  implicit
  oAuth2Persistence: TOAuth2Persistence
) {
  import OAuth2Service._

  logger.debug(s"Initializing with ${providers.map(_.name).mkString(",")}")(
    OAuth2LogContext()
  )

  /**
    * Get a reference to a provider by name.
    *
    * @param providerName
    * @return
    */
  def knownProvider(providerName: String): Option[OAuth2Provider] =
    providers.find(_.name == providerName)

  /**
    * Get the credential and provider definition for a given oAuth2Id.
    *
    * @param id
    * @param client
    * @param ec
    * @return
    */
  def get(id: OAuth2Id)(
    implicit client: StandaloneWSClient,
    ec: ExecutionContext,
    logContext: LogContext
  ): Future[(OAuth2Credential, OAuth2Provider)] = {
    val futureResponse = for {
      cred <- oAuth2Persistence
        .get(id)
        .flatMap(
          _.fold[Future[OAuth2Credential]](
            Future.failed[OAuth2Credential](
              new OAuth2Exception(s"Unable to find credential for $id")
            )
          )(Future.successful)
        )
      provider <- knownProvider(cred.providerName)
        .fold[Future[OAuth2Provider]](
          Future.failed[OAuth2Provider](
            new OAuth2Exception(
              s"Unable to find credential for ${cred.providerName}"
            )
          )
        )(Future.successful)
    } yield (cred, provider)

    futureResponse.flatMap {
      case (cred, baseProvider) =>
        val provider = cred.credentialsOverride match {
          case None    => baseProvider
          case Some(o) => baseProvider.withOverride(o)
        }
        val refreshedResponse =
          if (cred.accessExpires.forall(_.isAfter(SSDateTime.Instant.now))) {
            logger.trace(
              s"Access token for oid${cred.id} valid, expires ${cred.accessExpires}"
            )
            Future.successful((cred, provider))
          } else {
            logger.debug(
              s"Access token for oid${cred.id} expired ${cred.accessExpires}, refreshing via ${provider.tokenUrl}"
            )
            refresh(id).map { cred =>
              (cred, provider)
            }
          }
        refreshedResponse map {
          case (c, p) if c.credentialsOverride.isDefined =>
            (c, p.withOverride(c.credentialsOverride.get))
          case x => x
        }
    }
  }

  /**
    * Delete an OAuth2 credential.
    *
    * @param id
    * @return
    */
  def delete(id: OAuth2Id)(implicit ec: ExecutionContext,
                           lc: LogContext): Future[Int] =
    oAuth2Persistence.delete(id)

  /**
    * Log in via a username and password.
    *
    * @param oAuth2Id
    * @param username
    * @param password
    * @param client
    * @param ec
    * @return
    */
  def login(oAuth2Id: OAuth2Id, username: String, password: String)(
    implicit client: StandaloneWSClient,
    ec: ExecutionContext,
    logContext: LogContext
  ): Future[OAuth2Credential] = synchronized {
    val updatedCredOptT = for {
      cred <- OptionT(oAuth2Persistence.get(oAuth2Id))
      provider <- wrapOpt(knownProvider(cred.providerName))
      token <- wrapFut(
        OAuth2Service.passwordGrantAccessToken(provider, username, password)
      )
      updatedCred <- wrapFut(oAuth2Persistence.save(cred.copy(id = oAuth2Id)))
    } yield updatedCred
    updatedCredOptT.getOrElseF(
      Future.failed(
        new OAuth2Exception(s"Unable to locate credential for $oAuth2Id")
      )
    )
  }

  /**
    * Refresh the token for id.
    *
    * @param oAuth2Id
    * @param client
    * @param ec
    * @return
    */
  def refresh(oAuth2Id: OAuth2Id)(
    implicit client: StandaloneWSClient,
    ec: ExecutionContext,
    logContext: LogContext
  ): Future[OAuth2Credential] =
    refreshFutures.synchronized(
      refreshFutures.getOrElseUpdate(
        oAuth2Id,
        oAuth2Persistence.get(oAuth2Id).flatMap { credOpt =>
          logger.debug(s"Refreshing credentials for oid${oAuth2Id.id} $credOpt")
          credOpt
            .flatMap {
              cred =>
                knownProvider(cred.providerName).map {
                  baseProvider =>
                    val provider = cred.credentialsOverride match {
                      case None    => baseProvider
                      case Some(o) => baseProvider.withOverride(o)
                    }
                    OAuth2Service
                      .refresh(provider, cred)
                      .flatMap {
                        refreshed =>
                          oAuth2Persistence
                            .save(refreshed)
                            .map { saved =>
                              logger.trace(
                                s"Refreshed credentials for oid${oAuth2Id.id}, saving and clearing queue"
                              )
                              refreshFutures -= oAuth2Id
                              saved
                            }
                            .recoverWith {
                              case e =>
                                logger.debug(
                                  s"Failed to refresh credentials for oid${oAuth2Id.id}, clearing queue",
                                  e
                                )
                                refreshFutures -= oAuth2Id
                                Future.failed(e)
                            }
                      }
                }
            }
            .getOrElse(
              Future.failed(
                new OAuth2Exception(
                  s"Unable to locate credential for oid$oAuth2Id"
                )
              )
            )
        }
      )
    )

  /**
    * Create or update a stored client credential.
    *
    * @param state
    * @param oAuth2TokenResponse
    * @return
    */
  def createOrUpdateCredentials(state: OAuth2State,
                                oAuth2TokenResponse: OAuth2TokenResponse)(
    implicit ec: ExecutionContext,
    logContext: LogContext
  ): Future[Either[String, OAuth2Credential]] =
    OAuth2Service.createOrUpdateCredentials(state, oAuth2TokenResponse)

  /**
    * Request an access token via a username and password
    *
    * @param provider
    * @param ec
    * @param wsClient
    * @return
    */
  def passwordGrantAccessToken(provider: OAuth2Provider,
                               username: String,
                               password: String)(
    implicit ec: ExecutionContext,
    logContext: LogContext,
    wsClient: StandaloneWSClient
  ): Future[OAuth2Credential] =
    OAuth2Service.passwordGrantAccessToken(provider, username, password)

  /**
    * Request an access token via a client credentials grant.
    *
    * @param provider
    * @param ec
    * @param wsClient
    * @return
    */
  def clientCredentialsGrantAccessToken(provider: OAuth2Provider)(
    implicit ec: ExecutionContext,
    logContext: LogContext,
    wsClient: StandaloneWSClient
  ): Future[OAuth2TokenResponse] =
    OAuth2Service.clientCredentialsAccessToken(provider)

  /**
    * Request an access token via a code grant.
    *
    * @param provider
    * @param callbackUrl
    * @param code
    * @param ec
    * @param wsClient
    * @return
    */
  def codeGrantAccessToken(provider: OAuth2Provider,
                           callbackUrl: String,
                           code: String)(
    implicit ec: ExecutionContext,
    logContext: LogContext,
    wsClient: StandaloneWSClient
  ): Future[OAuth2TokenResponse] =
    OAuth2Service.codeGrantAccessToken(provider, callbackUrl, code)

}
