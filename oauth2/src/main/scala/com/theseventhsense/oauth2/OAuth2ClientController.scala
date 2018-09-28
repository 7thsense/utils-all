package com.theseventhsense.oauth2

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import cats.data.EitherT
import cats.implicits._
import io.circe.syntax._
import javax.inject.{Inject, Named}
import play.api.libs.circe._
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc._

import com.theseventhsense.oauth2.OAuth2Codecs._
import com.theseventhsense.utils.logging.{Logging, LogContext}
import com.theseventhsense.utils.oauth2.models.OAuth2State

class OAuth2Request[A](val provider: OAuth2Provider, request: Request[A])
    extends WrappedRequest[A](request)


object OAuth2ClientController {
  def cookieState(
    cookieName: String = DEFAULT_STATE_COOKIE_NAME
  )(implicit request: RequestHeader): Option[String] = {
    request.cookies.get(cookieName).map(_.value)
  }
}

class OAuth2ClientController @Inject()(
  @Named(CALLBACK_URL_PATH_KEY) callbackUrlPath: String,
  @Named(COOKIE_NAME_KEY) stateCookieName: String,
  stateMapper: TOAuth2StateMapper,
  oAuth2Service: OAuth2Service,
  override val controllerComponents: ControllerComponents
)(implicit val wsClient: StandaloneWSClient)
    extends BaseController
    with Circe
    with Logging {
  implicit val ec: ExecutionContext = controllerComponents.executionContext

  import OAuth2ClientController._

  def callbackUrl[A](request: OAuth2Request[A],
                     path: String,
                     state: Option[OAuth2State] = None): String = {
    val baseUrl = s"https://${request.host}$path"
      .replace(":providerName", request.provider.name)
    state match {
      case None => baseUrl
      case Some(s) =>
        val params = Map("state" -> s.id)
        baseUrl + OAuth2Service.encode(params)
    }
  }

  protected def action(
    providerName: String
  ): ActionBuilder[OAuth2Request, AnyContent] =
    new ActionBuilder[OAuth2Request, AnyContent] {
      override def parser: BodyParser[AnyContent] =
        controllerComponents.parsers.default
      override def executionContext: ExecutionContext =
        controllerComponents.executionContext

      def invokeBlock[A](
        request: Request[A],
        block: OAuth2Request[A] => Future[Result]
      ): Future[Result] = {
        oAuth2Service.knownProvider(providerName) match {
          case None =>
            Future.successful(
              NotFound(
                Map(
                  "error" -> "invalid_provider",
                  s"message" -> s"No provider found for $providerName"
                ).asJson
              )
            )
          case Some(provider: OAuth2Provider) =>
            block(new OAuth2Request(provider, request))
        }
      }
    }

  protected def getStateCookie(stateId: String): Cookie = {
    Cookie(name = stateCookieName, value = stateId, httpOnly = true)
  }

  protected def discardingCookie: DiscardingCookie = {
    DiscardingCookie(stateCookieName)
  }

  /**
    * The auth controller is the application-side interface to the oauth2 service.
    *
    * It provides a simple way to fire a redirect to the appropriate oauth2 provider,
    * setting the parameters necessary for the oauth2 service to hand the finalized connection
    * back to the application.
    *
    * It also stores any necessary application side information in the oAuth2State object, allowing
    * it to be used when the connection is finalized. If no auth url is configured, the user is
    * redirected directly to the callback url, allowing the token generation to be completed
    * immediately.
    *
    * @param providerName
    * @return
    */
  def auth(providerName: String): Action[AnyContent] = action(providerName) {
    implicit request =>
      val next = request.getQueryString("next")
      val bindUrl = request.getQueryString("bindUrl")
      val oAuth2Override = OAuth2CredentialsOverride.fromRequest(request)
      val oAuth2Id = request
        .getQueryString("oAuth2Id")
        .flatMap(id => Try(id.toLong).toOption)
        .map(OAuth2Id(_))
      val offline = request
        .getQueryString("offline")
        .getOrElse("true") == "true"
      val forceApproval = request
        .getQueryString("forceApproval")
        .getOrElse("true") == "true"
      val loginHint = request.getQueryString("loginHint")
      val params = request.provider.authParams.map { param: AuthParam =>
        param.name -> request.getQueryString(param.name)
      }
      val extraParams = params
        .filter(_._2.isDefined)
        .map { param =>
          param._1 -> param._2.get
        }
        .toMap
      if (params.count(_._2.isDefined) != params.length) {
        val missing = params.filter(_._2.isEmpty).map(_._1)
        BadRequest(
          Map(
            "error" -> "missing_parameters",
            "message" ->
              s"""missing required parameters: ${missing
                .mkString(",")}"""
          ).asJson
        )
      } else {
        val provider = oAuth2Override match {
          case Some(o) => request.provider.withOverride(o)
          case None    => request.provider
        }
        val state =
          stateMapper.create(provider, oAuth2Id, next, bindUrl, oAuth2Override)
        val url = provider.authUrl match {
          // Redirect straight to the callback if there is no authorization url
          case None => callbackUrl(request, callbackUrlPath, Some(state))
          // Redirect to the providers auth url if one exists
          case Some(authUrl) =>
            OAuth2Service.authRequestURL(
              provider = provider,
              callbackUrl = callbackUrl(request, callbackUrlPath),
              state = state,
              offline = offline,
              forceApproval = forceApproval,
              loginHint = loginHint,
              extraParams = extraParams,
              next = next
            )(OAuth2LogContext())
        }
        var response = Found(url)
        if (request.provider.flags.contains(RequiresStateCookie)) {
          response = response.withCookies(getStateCookie(state.id))
        }
        response
      }
  }

  /**
    * The callback controller handles actually extracting a token from a provider. This top-level method
    * concerns itself only with figuring out which state to associate the token with by using the state
    * query string parameter or state cookie if necessary.
    *
    * Actual validation and execution (if necessary) of the callback is delegated to handleCallback().
    *
    * @param providerName
    * @return
    */
  def callback(providerName: String): Action[AnyContent] =
    action(providerName).async { implicit request =>
      val codeOpt = request.getQueryString("code")
      val stateOpt = request.getQueryString("state")
      stateOpt.orElse(cookieState(stateCookieName)) match {
        case None =>
          Future.successful(
            BadRequest(
              Map(
                "error" -> "missing_state",
                "message" -> "no oauth2 state provided"
              ).asJson
            )
          )
        case Some(state: String) =>
          stateMapper.getById(state) match {
            case None =>
              Future.successful(
                BadRequest(
                  Map(
                    "error" -> "invalid state",
                    "message" -> s"no oauth2 state found for $state"
                  ).asJson
                )
              )
            case Some(state: OAuth2State) =>
              handleCallback(state, codeOpt, request)
          }
      }
    }

  /**
    * Delegate the various types of callbacks based on the provider configuration and presence of query
    * string parameters.
    *
    * @param state
    * @param codeOpt
    * @param request
    * @return
    */
  private def handleCallback(
    state: OAuth2State,
    codeOpt: Option[String],
    request: OAuth2Request[AnyContent]
  ): Future[Result] = codeOpt match {
    case Some(code: String) =>
      handleCodeCallback(state, request, code)
    case None
        if request.provider.flags
          .contains(RefreshModeClientCredentials) && state.oAuth2Override.isDefined =>
      handleClientCredentialsCallback(state, request)
    case None if request.provider.flags.contains(SkipsTokenRequest) =>
      handleQueryStringCallback(state, request)
    case None =>
      handleCallbackFailed(
        state,
        request,
        Some("required code parameter not found")
      )
  }

  /**
    * If a code was provided, use it to request an access token from the provider. This is the standard
    * 3-legged OAuth flow.
    */
  private def handleCodeCallback(state: OAuth2State,
                                 request: OAuth2Request[AnyContent],
                                 code: String): Future[Result] = {
    implicit val logContext: LogContext = OAuth2LogContext(
      context = state.oAuth2Id.map(id => "oAuth2Id" -> id.id.toString).toMap
    )
    oAuth2Service
      .codeGrantAccessToken(
        request.provider,
        callbackUrl(request, callbackUrlPath),
        code
      )
      .flatMap { response =>
        logger.trace(
          s"Successfully received callback for oauth2 state ${state.id}"
        )
        handleCallbackSuccess(state, response)
      }
      .recoverWith {
        case t =>
          logger.warn("Failed constructing code grant access token", t)
          Future.failed(t)
      }
  }

  /**
    * If there's no code, and this provider is configured for 2-legged oauth (client credentials grants),
    * we'll check and see if the state has the necessary client credentials available and use them.
    */
  private def handleClientCredentialsCallback(
    state: OAuth2State,
    request: OAuth2Request[AnyContent]
  ): Future[Result] = {
    implicit val logContext: LogContext = OAuth2LogContext(
      context = state.oAuth2Id.map(id => "oAuth2Id" -> id.id.toString).toMap
    )
    val provider = request.provider.withOverride(state.oAuth2Override.get)
    oAuth2Service
      .clientCredentialsGrantAccessToken(provider)
      .flatMap { response =>
        logger.trace(
          s"Successfully logged in with client credentials for ${state.id}"
        )
        handleCallbackSuccess(state, response)
      }
      .recoverWith {
        case t =>
          logger.warn(
            "Failed constructing client credentials grant access token",
            t
          )
          Future.failed(t)
      }
  }

  /**
    * If there was no code and the provider was configured for it, extract the access_token directly from the
    * incoming callback url. This is non-standard oauth behavior used by Hubspot.
    *
    * @param state
    * @param request
    * @return
    */
  private def handleQueryStringCallback(
    state: OAuth2State,
    request: OAuth2Request[AnyContent]
  ): Future[Result] = {
    request.getQueryString("access_token") match {
      case Some(access_token) =>
        val response = OAuth2TokenResponse(
          id = Some(state.id),
          access_token = access_token,
          refresh_token = request.getQueryString("refresh_token")
        )
        handleCallbackSuccess(state, response)
      case None =>
        handleCallbackFailed(
          state,
          request,
          Some("required access_token parameter not found")
        )
    }
  }

  /**
    * Generate a callback success message regardless of the extraction method
    *
    * @param state
    * @param response
    * @return
    */
  private def handleCallbackSuccess(
    state: OAuth2State,
    response: OAuth2TokenResponse
  ): Future[Result] = {
    implicit val logContext: LogContext = OAuth2LogContext(
      context = state.oAuth2Id.map(id => "oAuth2Id" -> id.id.toString).toMap
    )
    (for {
      creds <- EitherT(oAuth2Service.createOrUpdateCredentials(state, response))
      provider <- EitherT.fromOption[Future](
        oAuth2Service.knownProvider(creds.providerName),
        s"Unknown provider ${creds.providerName}"
      )
      infoParams <- EitherT.liftF[Future, String, Map[String, String]](
        creds.refreshToken match {
          case Some(refreshToken) =>
            OAuth2Service.refreshTokenInfo(provider, refreshToken)
          case None =>
            Future.successful(Map.empty[String, String])
        }
      )
    } yield
      if (state.bindUrl.isDefined) {
        val idTokenClaims = (for {
          extractor <- provider.idTokenExtractor
          id_token <- response.id_token
        } yield extractor(id_token)).getOrElse(Map.empty)
        val params = Map("oAuth2Id" -> creds.id.toString) ++
          state.next.map("next" -> _) ++ infoParams ++ idTokenClaims
        Redirect(state.bindUrl.get + OAuth2Service.encode(params))
          .discardingCookies(discardingCookie)
      } else if (state.next.isDefined) {
        Redirect(state.next.get)
          .discardingCookies(discardingCookie)
      } else {
        Ok(creds.asJson)
      }).value.map {
      case Right(result) => result
      case Left(error) =>
        InternalServerError(
          Map("error" -> "server_error", "message" -> error).asJson
        )
    }
  }

  /**
    * No code was provided and no fallback configured. Error out.
    */
  private def handleCallbackFailed(
    state: OAuth2State,
    request: RequestHeader,
    message: Option[String] = None
  ): Future[Result] = {
    implicit val logContext: LogContext = OAuth2LogContext(
      context = state.oAuth2Id.map(id => "oAuth2Id" -> id.id.toString).toMap
    )
    val error = request
      .getQueryString("error")
      .orElse(message)
      .getOrElse("invalid callback")
    val queryParams = Map("error" -> "INVALID_CALLBACK", "message" -> error)
    state.oAuth2Id
      .map(oAuth2Service.delete)
      .getOrElse(Future.successful(0))
      .map { deleted =>
        if (state.bindUrl.isDefined) {
          val params = Map("action" -> "DELETE") ++
            queryParams ++
            state.next.map("next" -> _) ++
            state.oAuth2Id.map("oAuth2Id" -> _)
          Redirect(state.bindUrl.get + OAuth2Service.encode(params))
            .discardingCookies(discardingCookie)
        } else if (state.next.isDefined) {
          Redirect(state.next.get + OAuth2Service.encode(queryParams))
            .discardingCookies(discardingCookie)
        } else {
          Ok(queryParams.asJson)
        }
      }
  }
}
