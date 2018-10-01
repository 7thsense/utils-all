package com.theseventhsense.utils.retry

import com.theseventhsense.utils.logging.{LogContext, Logging}
import play.api.PlayException
import scala.concurrent.{ExecutionContext, Future}

import com.theseventhsense.utils.models.TLogContext

/**
  * Created by erik on 3/2/16.
  */
class FutureImmediateRetryStrategy(
  maxCount: Int = 10
)
    extends FutureRetryStrategy
    with Logging {

  override def retry[T](producer: => Future[T])(implicit ec: ExecutionContext, lc: TLogContext): Future[T] =
    recursiveRetry(producer)

  def recursiveRetry[T](producer: => Future[T], count: Long = 1L)(implicit ec: ExecutionContext, lc: TLogContext): Future[T] = {
    producer.recoverWith {
      case t =>
        val newT =
          new PlayException(
            s"Failed ${t.getMessage}, retry $count/$maxCount",
            t.getMessage,
            t
          )
        if (count < maxCount) {
          logger.trace(newT.getMessage, newT)
          recursiveRetry(producer, count + 1L)
        } else {
          Future.failed(newT)
        }
    }
  }
}
