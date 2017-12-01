package com.theseventhsense.clients.wsclient

import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.implicits._
import com.theseventhsense.clients.wsclient.RestClient.RestResponseDecoder
import com.theseventhsense.utils.logging.Logging
import com.theseventhsense.utils.persistence.Keyed
import com.theseventhsense.utils.retry.NoOpRetryStrategy
import com.theseventhsense.utils.throttle.Throttle
import io.circe.{Decoder, parser}
import play.api.libs.json._
import play.api.libs.ws.{
  BodyWritable,
  InMemoryBody,
  WSClient,
  WSRequest,
  WSResponse
}
import play.api.mvc.{Codec, MultipartFormData}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object RestClient {
  abstract class RestResponseDecoder[T] {
    def parseResponse(request: WSRequest,
                      response: WSResponse): Either[RestClientException, T]
  }

  class PlayJsonResponseDecoder[T](implicit format: Format[T])
      extends RestResponseDecoder[T] {
    def parseResponse(request: WSRequest,
                      response: WSResponse): Either[RestClientException, T] = {
      Try(response.json) match {
        case Success(json: JsValue) =>
          json.validate[T] match {
            case JsSuccess(s, _) =>
              Right(s)
            case e: JsError =>
              Left(
                RestClientException
                  .DecodeFailure(
                    request.url,
                    Json.prettyPrint(json),
                    Json.prettyPrint(JsError.toJson(e))
                  )
              )
          }
        case Failure(t) =>
          Left(
            RestClientException
              .DecodeFailure(request.url, response.body, t.getMessage)
          )
      }
    }
  }

  class CirceResponseDecoder[T](implicit decoder: Decoder[T])
      extends RestResponseDecoder[T] {
    def parseResponse(request: WSRequest,
                      response: WSResponse): Either[RestClientException, T] = {
      parser
        .decode[T](response.body)
        .leftMap(
          e =>
            RestClientException
              .DecodeFailure(request.url, response.body, e.getMessage)
        )
    }
  }

  implicit def playJsonResponseDecoder[T](
    implicit format: Format[T]
  ): RestResponseDecoder[T] =
    new PlayJsonResponseDecoder[T]

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

class RestClient(
  val wsClient: WSClient,
  oAuth2Client: OAuth2WSClient,
  throttle: Throttle,
  retryStrategy: RestClientRetryStrategy = new NoOpRetryStrategy[WSResponse](),
  onFailure: Throwable => Future[WSResponse] = t => Future.failed(t)
)(implicit val ec: ExecutionContext)
    extends Logging {

  def executeWithRetry(request: WSRequest): Future[WSResponse] = {
    val retriedResponse = retryStrategy.retry({
      val requestWithAuth = oAuth2Client.executeWithAuth(request)
      throttle.executeThrottled(requestWithAuth)
    })
    retriedResponse
  }.recoverWith { case t => onFailure(t) }

  def postMultipartWithRetry(
    request: WSRequest,
    body: Source[MultipartFormData.Part[Source[ByteString, _]], _]
  ): Future[WSResponse] = {
    val retriedResponse = retryStrategy.retry({
      val requestWithAuth = oAuth2Client.postMultipartWithAuth(request, body)
      throttle.executeThrottled(requestWithAuth)
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

  def parseResponse[T](request: WSRequest, response: WSResponse)(
    implicit restResponseDecoder: RestResponseDecoder[T]
  ): Either[RestClientException, T] = {
    restResponseDecoder.parseResponse(request, response)
  }

  def executeJson[T](
    request: WSRequest
  )(implicit restResponseDecoder: RestResponseDecoder[T]): Future[T] = {
    val url = s"${request.url}?${queryString(request.queryString)}"
    logger.trace(
      s"loading from ${request.url}?${queryString(request.queryString)}"
    )
    executeWithRetry(request).flatMap { response: WSResponse =>
      response.status match {
        case 200 =>
          logger.trace(s"loaded $url: ${response.body.take(2048)}")
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
            RestClientException.Unknown(request.url, s"invalid status code $c")
          )
      }
    }
  }
}
