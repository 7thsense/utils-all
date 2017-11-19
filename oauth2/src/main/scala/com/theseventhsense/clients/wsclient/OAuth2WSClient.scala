package com.theseventhsense.clients.wsclient

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.theseventhsense.oauth2._
import com.theseventhsense.utils.logging.Logging
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.mvc.MultipartFormData

import scala.concurrent.{ExecutionContext, Future}

class OAuth2WSClient(id: OAuth2Id)(implicit context: OAuth2WSClient.Context)
    extends Logging {
  implicit def client: WSClient = context.wsClient
  implicit def ec: ExecutionContext = context.ec

  def executeWithAuth(request: WSRequest): Future[WSResponse] = {
    context.oAuth2Service
      .get(id)
      .flatMap {
        case (creds, provider) =>
          val requestWithAuth =
            OAuth2WSClient.withAuthorization(
              id,
              request,
              creds.accessToken,
              provider
            )
          requestWithAuth.execute() flatMap { (response: WSResponse) =>
            if (provider.responseHandler.shouldRefresh(response)) {
              context.oAuth2Service.refresh(id).flatMap { credential =>
                val refreshedRequest =
                  OAuth2WSClient.withAuthorization(
                    id,
                    request,
                    credential.accessToken,
                    provider
                  )
                refreshedRequest.execute()
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

  def postMultipartWithAuth(
    request: WSRequest,
    body: Source[MultipartFormData.Part[Source[ByteString, _]], _]
  ): Future[WSResponse] = {
    context.oAuth2Service
      .get(id)
      .flatMap {
        case (creds, provider) =>
          val requestWithAuth =
            OAuth2WSClient.withAuthorization(
              id,
              request,
              creds.accessToken,
              provider
            )
          requestWithAuth.post(body) flatMap { (response: WSResponse) =>
            if (provider.responseHandler.shouldRefresh(response)) {
              context.oAuth2Service.refresh(id).flatMap { credential =>
                val refreshedRequest =
                  OAuth2WSClient.withAuthorization(
                    id,
                    request,
                    credential.accessToken,
                    provider
                  )
                refreshedRequest.post(body)
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

object OAuth2WSClient extends Logging {

  case class Context(wsClient: WSClient,
                     ec: ExecutionContext,
                     oAuth2Service: OAuth2Service)

  def connect(id: OAuth2Id)(implicit context: Context): OAuth2WSClient = {
    new OAuth2WSClient(id)
  }

  def withAuthorization(
    oAuth2Id: OAuth2Id,
    request: WSRequest,
    accessToken: String,
    provider: OAuth2Provider
  )(implicit context: Context): WSRequest = {
    implicit val ec: ExecutionContext = context.ec
    implicit val wsClient: WSClient = context.wsClient
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
}
