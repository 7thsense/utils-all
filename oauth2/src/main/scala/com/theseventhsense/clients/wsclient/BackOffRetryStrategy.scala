package com.theseventhsense.clients.wsclient

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import akka.actor.ActorSystem
import play.api.libs.ws.StandaloneWSResponse

import com.theseventhsense.oauth2.OAuth2Service
import com.theseventhsense.utils.logging.{LogContext, Logging}
import com.theseventhsense.utils.models.TLogContext
import com.theseventhsense.utils.retry.FutureBackOffRetryStrategy

sealed trait RetryFlags {
  def shouldRetry(response: StandaloneWSResponse): Boolean
}

object RetryFlags {
  case object Http401 extends RetryFlags {
    def shouldRetry(response: StandaloneWSResponse): Boolean = {
      response.status == 401
    }
  }

  case object Http409 extends RetryFlags {
    def shouldRetry(response: StandaloneWSResponse): Boolean = {
      response.status == 409
    }
  }

  case object Http429 extends RetryFlags {
    def shouldRetry(response: StandaloneWSResponse): Boolean = {
      response.status == 429
    }
  }

  case object Http500 extends RetryFlags {
    def shouldRetry(response: StandaloneWSResponse): Boolean = {
      response.status == 500
    }
  }

  case object Http502 extends RetryFlags {
    def shouldRetry(response: StandaloneWSResponse): Boolean = {
      response.status == 502
    }
  }

  case object Http503 extends RetryFlags {
    def shouldRetry(response: StandaloneWSResponse): Boolean = {
      response.status == 503
    }
  }

  case object Http504 extends RetryFlags {
    def shouldRetry(response: StandaloneWSResponse): Boolean = {
      response.status == 504
    }
  }

}

object BackOffRetryStrategy {

  class RetryableException(flags: Set[RetryFlags]) extends Throwable {
    override def getMessage: String = s"Retryable failure: $flags"
  }

  def shouldRetryThrowable(t: Throwable): Boolean = t match {
    case _: OAuth2Service.BadRequestError => false
    case _: OAuth2Service.DecodeError     => false
    case _                                => true
  }

}

class BackOffRetryStrategy(
  flags: Set[RetryFlags],
  firstDelay: FiniteDuration,
  maxCount: Int = 10,
  maxDelay: FiniteDuration = 1.hour,
  shouldRetryThrowable: Throwable => Boolean =
    BackOffRetryStrategy.shouldRetryThrowable
)(implicit system: ActorSystem)
    extends RestClientRetryStrategy
    with Logging {

  import BackOffRetryStrategy._

  val genericRetryStrategy =
    new FutureBackOffRetryStrategy(
      firstDelay,
      maxCount,
      maxDelay,
      shouldRetry = shouldRetryThrowable
    )

  protected def shouldRetryResponse(
    response: StandaloneWSResponse
  ): Future[StandaloneWSResponse] = {
    val shouldFlags = flags.filter(_.shouldRetry(response))
    if (shouldFlags.isEmpty) {
      Future.successful(response)
    } else {
      Future.failed(new RetryableException(shouldFlags))
    }
  }

  override def retry(producer: => Future[StandaloneWSResponse])(
    implicit ec: ExecutionContext,
    lc: TLogContext
  ): Future[StandaloneWSResponse] =
    genericRetryStrategy.retry(producer.flatMap(shouldRetryResponse))

}
