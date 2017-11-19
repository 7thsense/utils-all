package com.theseventhsense.testing

import cats.data._
import cats.scalatest.EitherValues
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

/**
  * Created by erik on 2/25/16.
  */
trait MonadTransformerValues
    extends ScalaFutures
    with OptionValues
    with EitherValues {
  implicit class FutureOptionValue[T](v: Future[Option[T]]) {
    def fValue: T = v.futureValue.value
  }
  implicit class OptionTFutureValue[T](v: OptionT[Future, T]) {
    def fValue: T = v.value.futureValue.value
  }

  implicit class FutureEitherValue[A, B](v: Future[Either[A, B]]) {
    def fValue: B = v.futureValue.value
  }
  implicit class EitherTFutureValue[A, B](v: EitherT[Future, A, B]) {
    def fValue: B = v.value.futureValue.value
  }
}

object MonadTransformerValues extends MonadTransformerValues

trait FutureValues extends ScalaFutures {
  implicit class FutureValue[T](v: Future[T]) {
    def fValue: T = v.futureValue
  }
}

object FutureValues extends FutureValues
