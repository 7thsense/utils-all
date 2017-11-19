package com.theseventhsense.utils.retry

import scala.concurrent.Future

trait FutureRetryStrategyWithCriteria {
  def retry[T](producer: => Future[T],
               passCriteria: (T) => Boolean,
               failCriteria: (Throwable) => Boolean): Future[T]
}

trait FutureRetryStrategy {
  def retry[T](producer: => Future[T]): Future[T]
}
