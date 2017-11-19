package com.theseventhsense.utils.retry

import scala.concurrent.Future

trait RetryStrategy[T] {
  def retry(producer: => Future[T]): Future[T]
}

class NoOpRetryStrategy[T] extends RetryStrategy[T] {
  def retry(producer: => Future[T]): Future[T] = {
    producer
  }
}
