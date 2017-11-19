package com.theseventhsense.utils.cats

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
  * Created by erik on 4/21/16.
  */
package object syntax {
  implicit class RichFutureEither[T, E <: Throwable](fx: Future[Either[E, T]]) {
    def flattenEither(implicit ec: ExecutionContext): Future[T] = fx.flatMap {
      case Left(e) => Future.failed(e)
      case Right(t) => Future.successful(t)
    }
  }
}
