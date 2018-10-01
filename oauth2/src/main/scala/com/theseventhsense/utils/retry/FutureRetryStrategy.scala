package com.theseventhsense.utils.retry

import scala.concurrent.{ExecutionContext, Future}

import com.theseventhsense.utils.logging.LogContext
import com.theseventhsense.utils.models.TLogContext

trait FutureRetryStrategyWithCriteria {
  def retry[T](producer: => Future[T],
               passCriteria: T => Boolean,
               failCriteria: Throwable => Boolean)(implicit ec: ExecutionContext, lc: TLogContext): Future[T]
}

trait FutureRetryStrategy {
  def retry[T](producer: => Future[T])(implicit ec: ExecutionContext, lc: TLogContext): Future[T]
}
