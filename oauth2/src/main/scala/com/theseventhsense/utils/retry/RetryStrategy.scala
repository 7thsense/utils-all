package com.theseventhsense.utils.retry

import scala.concurrent.{ExecutionContext, Future}

import com.theseventhsense.utils.models.TLogContext

trait RetryStrategy[T] {
  def retry(producer: => Future[T])(implicit ec: ExecutionContext,
                                    lc: TLogContext): Future[T]
}

class NoOpRetryStrategy[T] extends RetryStrategy[T] {
  def retry(producer: => Future[T])(implicit ec: ExecutionContext,
                                    lc: TLogContext): Future[T] = {
    producer
  }
}
