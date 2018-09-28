package com.theseventhsense.utils.retry

import scala.concurrent.{ExecutionContext, Future}

import com.theseventhsense.utils.logging.LogContext

trait RetryStrategy[T] {
  def retry(producer: => Future[T])(implicit ec: ExecutionContext,
                                    lc: LogContext): Future[T]
}

class NoOpRetryStrategy[T] extends RetryStrategy[T] {
  def retry(producer: => Future[T])(implicit ec: ExecutionContext,
                                    lc: LogContext): Future[T] = {
    producer
  }
}
