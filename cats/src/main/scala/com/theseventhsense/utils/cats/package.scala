package com.theseventhsense.utils

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import _root_.cats.data._
import _root_.cats.implicits._

/**
  * Created by erik on 1/27/16.
  */
package object cats {
  def wrapSome[T](t: T): OptionT[Future, T] =
    OptionT(Future.successful(Option(t)))

  def wrapOpt[T](t: Option[T]): OptionT[Future, T] =
    OptionT(Future.successful(t))

  def wrapFut[T](t: Future[T])(
      implicit ec: ExecutionContext): OptionT[Future, T] =
    OptionT(t.map(Option(_)))

  def wrapOptAsEitherT[A, B](t: Option[A],
                             ifNone: => B): EitherT[Future, B, A] =
    EitherT(Future.successful(Either.fromOption(t, ifNone)))
}
