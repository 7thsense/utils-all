package com.theseventhsense.clients.wsclient

import scala.concurrent.{ExecutionContext, Future}

import akka.actor.ActorSystem
import cats.implicits._
import io.circe.{Decoder, parser}
import play.api.libs.ws._
import play.api.mvc.Codec

import com.theseventhsense.clients.wsclient.RestClient.RestResponseDecoder
import com.theseventhsense.utils.logging.{LogContext, Logging}
import com.theseventhsense.utils.models.TLogContext
import com.theseventhsense.utils.persistence.Keyed
import com.theseventhsense.utils.retry.NoOpRetryStrategy
import com.theseventhsense.utils.throttle.Throttle

object RestClient {
  abstract class RestResponseDecoder[T] {
    def parseResponse(
      request: StandaloneWSRequest,
      response: StandaloneWSResponse
    ): Either[RestClientException, T]
  }

  class CirceResponseDecoder[T](implicit decoder: Decoder[T])
      extends RestResponseDecoder[T] {
    def parseResponse(
      request: StandaloneWSRequest,
      response: StandaloneWSResponse
    ): Either[RestClientException, T] = {
      parser
        .decode[T](response.body)
        .leftMap(
          e =>
            RestClientException
              .DecodeFailure(request.url, response.body, e.getMessage)
        )
    }
  }

  implicit def circeResponseDecoder[T](
    implicit decoder: Decoder[T]
  ): RestResponseDecoder[T] =
    new CirceResponseDecoder[T]

  object Implicits {
    implicit def bodyWriteableOfCirceJson(
      implicit codec: Codec
    ): BodyWritable[io.circe.Json] =
      BodyWritable(
        obj => InMemoryBody(codec.encode(obj.noSpaces)),
        "application/json"
      )

    implicit def bodyWriteableOfCirceEncodeable[T](
      implicit codec: Codec,
      encoder: io.circe.Encoder[T]
    ): BodyWritable[T] =
      BodyWritable(
        obj => InMemoryBody(codec.encode(encoder(obj).noSpaces)),
        "application/json"
      )
  }
}

class RestClient(val wsClient: StandaloneWSClient,
                 oAuth2Client: OAuth2WSClient,
                 throttle: Throttle,
                 retryStrategy: RestClientRetryStrategy =
                   new NoOpRetryStrategy[StandaloneWSResponse](),
                 onFailure: Throwable => Future[StandaloneWSResponse] = t =>
                   Future.failed(t))
    extends Logging {

  def executeWithAuth(request: StandaloneWSRequest)(
    implicit ec: ExecutionContext,
    lc: TLogContext
  ): Future[StandaloneWSResponse] =
    oAuth2Client.executeWithAuth(request)

  def executeWithAuthThrottled(request: StandaloneWSRequest)(
    implicit ec: ExecutionContext,
    lc: TLogContext
  ): Future[StandaloneWSResponse] =
    throttle.executeThrottled(oAuth2Client.executeWithAuth(request))

  def executeWithAuthThrottledRetry(request: StandaloneWSRequest)(
    implicit ec: ExecutionContext,
    lc: TLogContext
  ): Future[StandaloneWSResponse] = {
    val retriedResponse = retryStrategy.retry({
      executeWithAuthThrottled(request)
    })
    retriedResponse
  }.recoverWith { case t => onFailure(t) }

  def postWithAuth[T: BodyWritable](request: StandaloneWSRequest, body: T)(
    implicit ec: ExecutionContext,
    lc: TLogContext
  ): Future[StandaloneWSResponse] =
    oAuth2Client.postWithAuth(request, body)

  def postWithAuthThrottled[T: BodyWritable](request: StandaloneWSRequest,
                                             body: T)(
    implicit ec: ExecutionContext,
    lc: TLogContext
  ): Future[StandaloneWSResponse] =
    throttle.executeThrottled(oAuth2Client.postWithAuth(request, body))

  def postWithAuthThrottledRetry[T: BodyWritable](request: StandaloneWSRequest,
                                                  body: T)(
    implicit ec: ExecutionContext,
    lc: TLogContext
  ): Future[StandaloneWSResponse] = {
    val retriedResponse = retryStrategy.retry({
      postWithAuthThrottled(request, body)
    })
    retriedResponse
  }.recoverWith { case t => onFailure(t) }

  def queryString(parameters: Map[String, Seq[String]]): String = {
    parameters
      .map {
        case (k, v) =>
          k + "=" + v.mkString(",")
      }
      .mkString("&")
  }

  def progressMessage[T](s: T): String = {
    val responseType = s.getClass.getSimpleName
    s"$responseType loaded"
  }

  def progressMessage[T <: Keyed](s: Batch[T]): String = {
    val responseType = s.getClass.getSimpleName
    if (s.items.nonEmpty) {
      s"$responseType of ${s.items.length} hasMore: ${s.hasMore}, " +
        s"next: ${s.nextOffset}}}}"
    } else {
      s"no $responseType remaining"
    }
  }

  def parseResponse[T](request: StandaloneWSRequest,
                       response: StandaloneWSResponse)(
    implicit restResponseDecoder: RestResponseDecoder[T]
  ): Either[RestClientException, T] = {
    restResponseDecoder.parseResponse(request, response)
  }

  def executeJsonWithAuthThrottledRetry[T](request: StandaloneWSRequest)(
    implicit
    ec: ExecutionContext,
    lc: TLogContext,
    restResponseDecoder: RestResponseDecoder[T]
  ): Future[T] = {
    val url = s"${request.url}?${queryString(request.queryString)}"
    executeWithAuthThrottledRetry(request).flatMap {
      response: StandaloneWSResponse =>
        response.status match {
          case 200 =>
            parseResponse[T](request, response) match {
              case Left(t) =>
                val ex = RestClientException.DecodeFailure(
                  url,
                  response.body,
                  t.getMessage
                )
                logger.trace(ex.detailedMessage)
                Future.failed(ex)
              case Right(x) =>
                Future.successful(x)
            }
          case 403 =>
            Future.failed(RestClientException.Forbidden(url, response.body))

          case c =>
            Future.failed(
              RestClientException
                .Unknown(request.url, s"invalid status code $c")
            )
        }
    }
  }
}
