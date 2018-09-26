package com.theseventhsense.clients.wsclient

import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import play.api.libs.ws._

import com.theseventhsense.oauth2._

case class OAuth2WSClient(id: OAuth2Id, context: OAuth2WSClient.Context) {
  import OAuth2WSClient._

  private implicit def logContext: LogContext = context.logContext
  private implicit def wsClient: StandaloneWSClient = context.wsClient

  def withLogContext(logContext: LogContext): OAuth2WSClient =
    copy(context = context.copy(logContext = logContext))

  def executeWithAuth(
    request: StandaloneWSRequest
  )(implicit ec: ExecutionContext): Future[StandaloneWSResponse] = {
    context.oAuth2Service
      .get(id)
      .flatMap {
        case (creds, provider) =>
          request
            .withAuthorization(id, creds.accessToken, provider)
            .withOptionalTraceLogging()
            .execute() flatMap { response: StandaloneWSResponse =>
            if (provider.responseHandler.shouldRefresh(response)) {
              context.oAuth2Service.refresh(id).flatMap { credential =>
                request
                  .withAuthorization(id, credential.accessToken, provider)
                  .execute()
              }
            } else {
              Future.successful(response)
            }
          }
      }
      .recoverWith {
        case t =>
          logger.trace(s"Failed to get oAuth2 credentials for $id", t)
          Future.failed(t)
      }
  }

  def postWithAuth[T: BodyWritable](request: StandaloneWSRequest, body: T)(
    implicit ec: ExecutionContext
  ): Future[StandaloneWSResponse] = {
    context.oAuth2Service
      .get(id)
      .flatMap {
        case (creds, provider) =>
          request
            .withAuthorization(id, creds.accessToken, provider)
            .withOptionalTraceLogging()
            .post(body) flatMap { response: StandaloneWSResponse =>
            if (provider.responseHandler.shouldRefresh(response)) {
              context.oAuth2Service.refresh(id).flatMap { credential =>
                request
                  .withAuthorization(id, credential.accessToken, provider)
                  .withOptionalTraceLogging()
                  .post(body)
              }
            } else {
              Future.successful(response)
            }
          }
      }
      .recoverWith {
        case t =>
          logger.trace(s"Failed to get oAuth2 credentials for $id", t)
          Future.failed(t)
      }
  }
}

object OAuth2WSClient {
  private[wsclient] val logger: LoggerTakingImplicit[LogContext] =
    Logger.takingImplicit[LogContext](this.getClass.getName)
  private[wsclient] val wireLogger: LoggerTakingImplicit[LogContext] = Logger
    .takingImplicit[LogContext](this.getClass.getPackage.getName + ".wire")

  implicit class RichWSRequest(request: StandaloneWSRequest) {
    def withOptionalTraceLogging()(
      implicit logContext: LogContext
    ): StandaloneWSRequest = {
      if (logContext.shouldWireLog(request))
        request.withRequestFilter(new WSClientCurlRequestLogger(wireLogger))
      else request
    }

    def withAuthorization(oAuth2Id: OAuth2Id,
                          accessToken: String,
                          provider: OAuth2Provider): StandaloneWSRequest =
      if (provider.flags.contains(
            AuthorizationMechanismFlag.TokenAsQueryParameter
          )) {
        request.addQueryStringParameters("access_token" -> accessToken)
      } else if (provider.flags.contains(
                   AuthorizationMechanismFlag.HeaderAsToken
                 )) {
        request.addHttpHeaders("Authorization" -> s"Token $accessToken")
      } else {
        request.addHttpHeaders("Authorization" -> s"Bearer $accessToken")
      }

  }

  case class Context(wsClient: StandaloneWSClient,
                     oAuth2Service: OAuth2Service,
                     logContext: LogContext = WSClientLogContext())

  def connect(id: OAuth2Id)(implicit context: Context): OAuth2WSClient =
    OAuth2WSClient(id, context)

}
